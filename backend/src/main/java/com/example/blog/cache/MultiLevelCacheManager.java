package com.example.blog.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MultiLevelCacheManager implements CacheManager {

    private final CacheManager l1CacheManager;
    private final CacheManager l2CacheManager;
    private final KeyLock keyLock = new KeyLock();
    private final ConcurrentHashMap<String, Cache> caches = new ConcurrentHashMap<>();

    public MultiLevelCacheManager(CacheManager l1CacheManager, CacheManager l2CacheManager) {
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, cacheName -> {
            Cache l1Cache = l1CacheManager != null ? l1CacheManager.getCache(cacheName) : null;
            Cache l2Cache = l2CacheManager != null ? l2CacheManager.getCache(cacheName) : null;
            if (l1Cache == null) {
                return l2Cache;
            }
            if (l2Cache == null) {
                return l1Cache;
            }
            return new MultiLevelCache(cacheName, l1Cache, l2Cache, keyLock);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new LinkedHashSet<>();
        if (l1CacheManager != null) {
            names.addAll(l1CacheManager.getCacheNames());
        }
        if (l2CacheManager != null) {
            names.addAll(l2CacheManager.getCacheNames());
        }
        names.addAll(caches.keySet());
        return names;
    }
}
