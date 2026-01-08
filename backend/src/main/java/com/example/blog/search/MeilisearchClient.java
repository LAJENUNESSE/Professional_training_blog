package com.example.blog.search;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class MeilisearchClient {

    private static final Logger logger = LoggerFactory.getLogger(MeilisearchClient.class);

    private final RestClient meilisearchRestClient;
    private final SearchProperties searchProperties;
    private final AtomicBoolean indexReady = new AtomicBoolean(false);

    public boolean isEnabled() {
        return searchProperties.isEnabled() && searchProperties.getEngine() == SearchProperties.Engine.MEILISEARCH;
    }

    public void ensureIndex() {
        if (!isEnabled() || indexReady.get()) {
            return;
        }
        String index = searchProperties.getMeilisearch().getIndex();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("uid", index);
            payload.put("primaryKey", "id");
            meilisearchRestClient.post()
                    .uri("/indexes")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            indexReady.set(true);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                indexReady.set(true);
                return;
            }
            throw ex;
        }
    }

    public SearchResult search(String keyword, Pageable pageable) {
        ensureIndex();
        String index = searchProperties.getMeilisearch().getIndex();
        Map<String, Object> payload = new HashMap<>();
        payload.put("q", keyword);
        payload.put("offset", (int) pageable.getOffset());
        payload.put("limit", pageable.getPageSize());
        payload.put("attributesToHighlight", List.of("title", "summary", "content"));
        payload.put("attributesToCrop", List.of("summary", "content"));
        payload.put("cropLength", 120);
        payload.put("highlightPreTag", "<em>");
        payload.put("highlightPostTag", "</em>");

        JsonNode response = meilisearchRestClient.post()
                .uri("/indexes/{index}/search", index)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            return new SearchResult(List.of(), Map.of(), 0);
        }

        long totalHits = response.hasNonNull("totalHits")
                ? response.path("totalHits").asLong()
                : response.path("estimatedTotalHits").asLong();

        List<Long> ids = new ArrayList<>();
        Map<Long, SearchResult.Highlight> highlights = new HashMap<>();
        for (JsonNode hit : response.path("hits")) {
            long id = hit.path("id").asLong();
            ids.add(id);
            JsonNode formatted = hit.path("_formatted");
            if (formatted != null && !formatted.isMissingNode()) {
                String title = sanitizeHighlight(formatted.path("title").asText(null));
                String summary = sanitizeHighlight(formatted.path("summary").asText(null));
                String content = sanitizeHighlight(formatted.path("content").asText(null));
                if (title != null || summary != null || content != null) {
                    highlights.put(id, new SearchResult.Highlight(title, summary, content));
                }
            }
        }
        return new SearchResult(ids, highlights, totalHits);
    }

    public void upsertDocuments(List<ArticleSearchDocument> documents) {
        if (!isEnabled() || documents == null || documents.isEmpty()) {
            return;
        }
        ensureIndex();
        String index = searchProperties.getMeilisearch().getIndex();
        meilisearchRestClient.post()
                .uri("/indexes/{index}/documents?primaryKey=id", index)
                .body(documents)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteDocument(Long id) {
        if (!isEnabled() || id == null) {
            return;
        }
        ensureIndex();
        String index = searchProperties.getMeilisearch().getIndex();
        meilisearchRestClient.delete()
                .uri("/indexes/{index}/documents/{id}", index, id)
                .retrieve()
                .toBodilessEntity();
    }

    private String sanitizeHighlight(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String open = "__H_OPEN__";
        String close = "__H_CLOSE__";
        String sanitized = value.replace("<em>", open).replace("</em>", close);
        sanitized = sanitized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return sanitized.replace(open, "<mark>").replace(close, "</mark>");
    }
}
