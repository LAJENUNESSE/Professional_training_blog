package com.example.blog.search;

import com.example.blog.dto.response.ArticleDTO;
import com.example.blog.dto.response.ArticleSuggestionDTO;
import com.example.blog.entity.Article;
import com.example.blog.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ArticleSearchService.class);

    private final SearchProperties searchProperties;
    private final MeilisearchClient meilisearchClient;
    private final ArticleRepository articleRepository;

    public Page<ArticleDTO> search(String keyword, Pageable pageable) {
        if (!isSearchEnabled()) {
            return articleRepository.searchByKeyword(keyword, pageable).map(ArticleDTO::fromEntityList);
        }
        try {
            SearchResult result = meilisearchClient.search(keyword, pageable);
            if (result.ids().isEmpty()) {
                return Page.empty(pageable);
            }
            List<Article> articles = articleRepository.findByIdInAndStatus(result.ids(), Article.Status.PUBLISHED);
            Map<Long, ArticleDTO> dtoMap = articles.stream()
                    .map(ArticleDTO::fromEntityList)
                    .collect(Collectors.toMap(ArticleDTO::getId, dto -> dto));
            List<ArticleDTO> ordered = new ArrayList<>();
            for (Long id : result.ids()) {
                ArticleDTO dto = dtoMap.get(id);
                if (dto == null) {
                    continue;
                }
                SearchResult.Highlight highlight = result.highlights().get(id);
                if (highlight != null) {
                    dto.setHighlightTitle(highlight.title());
                    dto.setHighlightSummary(highlight.summary());
                    dto.setHighlightContent(highlight.content());
                }
                ordered.add(dto);
            }
            return new PageImpl<>(ordered, pageable, result.totalHits());
        } catch (Exception ex) {
            logger.warn("Search fallback to database due to error", ex);
            return articleRepository.searchByKeyword(keyword, pageable).map(ArticleDTO::fromEntityList);
        }
    }

    public void indexArticle(Long articleId) {
        if (!isSearchEnabled() || articleId == null) {
            return;
        }
        articleRepository.findById(articleId).ifPresent(article -> {
            if (article.getStatus() != Article.Status.PUBLISHED) {
                meilisearchClient.deleteDocument(articleId);
                return;
            }
            ArticleSearchDocument document = ArticleSearchDocument.fromEntity(article);
            meilisearchClient.upsertDocuments(List.of(document));
        });
    }

    public void deleteArticle(Long articleId) {
        if (!isSearchEnabled() || articleId == null) {
            return;
        }
        meilisearchClient.deleteDocument(articleId);
    }

    public void reindexAll() {
        if (!isSearchEnabled()) {
            return;
        }
        int batchSize = Math.max(1, searchProperties.getReindexBatchSize());
        int page = 0;
        Page<Article> pageData;
        do {
            pageData = articleRepository.findByStatusWithTags(Article.Status.PUBLISHED,
                    PageRequest.of(page, batchSize));
            if (!pageData.getContent().isEmpty()) {
                List<ArticleSearchDocument> documents = pageData.getContent().stream()
                        .map(ArticleSearchDocument::fromEntity)
                        .toList();
                meilisearchClient.upsertDocuments(documents);
            }
            page++;
        } while (!pageData.isEmpty());
    }

    public List<ArticleSuggestionDTO> suggest(String keyword, int size) {
        if (!isSearchEnabled()) {
            return articleRepository.searchByKeyword(keyword, PageRequest.of(0, size))
                    .map(article -> new ArticleSuggestionDTO(article.getId(), article.getTitle(), article.getSlug()))
                    .toList();
        }
        try {
            SearchResult result = meilisearchClient.search(keyword, PageRequest.of(0, size));
            if (result.ids().isEmpty()) {
                return List.of();
            }
            List<Article> articles = articleRepository.findByIdInAndStatus(result.ids(), Article.Status.PUBLISHED);
            Map<Long, Article> articleMap = articles.stream()
                    .collect(Collectors.toMap(Article::getId, article -> article));
            List<ArticleSuggestionDTO> suggestions = new ArrayList<>();
            for (Long id : result.ids()) {
                Article article = articleMap.get(id);
                if (article != null) {
                    suggestions.add(new ArticleSuggestionDTO(article.getId(), article.getTitle(), article.getSlug()));
                }
            }
            return suggestions;
        } catch (Exception ex) {
            logger.warn("Suggest fallback to database due to error", ex);
            return articleRepository.searchByKeyword(keyword, PageRequest.of(0, size))
                    .map(article -> new ArticleSuggestionDTO(article.getId(), article.getTitle(), article.getSlug()))
                    .toList();
        }
    }

    private boolean isSearchEnabled() {
        return searchProperties.isEnabled()
                && searchProperties.getEngine() == SearchProperties.Engine.MEILISEARCH;
    }

}
