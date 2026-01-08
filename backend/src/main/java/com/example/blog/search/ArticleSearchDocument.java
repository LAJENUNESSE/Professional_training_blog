package com.example.blog.search;

import com.example.blog.entity.Article;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSearchDocument(
        Long id,
        String title,
        String summary,
        String content,
        String slug,
        String categoryName,
        List<String> tagNames,
        String authorName,
        LocalDateTime publishedAt
) {

    public static ArticleSearchDocument fromEntity(Article article) {
        String categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        List<String> tagNames = article.getTags() != null
                ? article.getTags().stream().map(tag -> tag.getName()).toList()
                : List.of();
        String authorName = article.getAuthor() != null
                ? (article.getAuthor().getNickname() != null ? article.getAuthor().getNickname() : article.getAuthor().getUsername())
                : null;
        return new ArticleSearchDocument(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getContent(),
                article.getSlug(),
                categoryName,
                tagNames,
                authorName,
                article.getPublishedAt()
        );
    }
}
