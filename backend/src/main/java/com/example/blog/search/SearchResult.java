package com.example.blog.search;

import java.util.List;
import java.util.Map;

public record SearchResult(
        List<Long> ids,
        Map<Long, Highlight> highlights,
        long totalHits
) {

    public record Highlight(String title, String summary, String content) {
    }
}
