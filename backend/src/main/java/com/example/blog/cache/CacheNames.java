package com.example.blog.cache;

import java.util.Set;

public final class CacheNames {

    public static final String PUBLISHED_ARTICLES = "articles:published";
    public static final String ARTICLES_BY_CATEGORY = "articles:category";
    public static final String ARTICLES_BY_TAG = "articles:tag";
    public static final String HOT_ARTICLES = "articles:hot";
    public static final String CATEGORY_LIST = "categories:all";
    public static final String TAG_LIST = "tags:all";
    public static final String CATEGORY_EXISTS = "categories:exists";
    public static final String TAG_EXISTS = "tags:exists";

    private CacheNames() {
    }

    public static Set<String> all() {
        return Set.of(
                PUBLISHED_ARTICLES,
                ARTICLES_BY_CATEGORY,
                ARTICLES_BY_TAG,
                HOT_ARTICLES,
                CATEGORY_LIST,
                TAG_LIST,
                CATEGORY_EXISTS,
                TAG_EXISTS
        );
    }
}
