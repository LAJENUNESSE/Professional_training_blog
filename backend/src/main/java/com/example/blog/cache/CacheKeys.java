package com.example.blog.cache;

import org.springframework.data.domain.Pageable;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static String pageKey(Pageable pageable) {
        if (pageable == null) {
            return "page:0:10:unsorted";
        }
        String sort = pageable.getSort().isSorted() ? pageable.getSort().toString() : "unsorted";
        return "page:" + pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + sort;
    }

    public static String pageKey(Long id, Pageable pageable) {
        return "id:" + id + ":" + pageKey(pageable);
    }

    public static String sizeKey(int size) {
        return "size:" + size;
    }
}
