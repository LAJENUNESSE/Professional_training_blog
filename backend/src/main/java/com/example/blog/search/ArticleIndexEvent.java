package com.example.blog.search;

public record ArticleIndexEvent(Long articleId, Action action) {

    public enum Action {
        UPSERT,
        DELETE
    }

    public static ArticleIndexEvent upsert(Long articleId) {
        return new ArticleIndexEvent(articleId, Action.UPSERT);
    }

    public static ArticleIndexEvent delete(Long articleId) {
        return new ArticleIndexEvent(articleId, Action.DELETE);
    }
}
