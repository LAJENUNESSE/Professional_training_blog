package com.example.blog.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class MultiLevelCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCache.class);

    private final String name;
    private final Cache l1Cache;
    private final Cache l2Cache;
    private final KeyLock keyLock;

    public MultiLevelCache(String name, Cache l1Cache, Cache l2Cache, KeyLock keyLock) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.keyLock = keyLock;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return new Object[]{nativeCache(l1Cache), nativeCache(l2Cache)};
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper l1Value = safeGet(l1Cache, key);
        if (l1Value != null) {
            return l1Value;
        }
        ValueWrapper l2Value = safeGet(l2Cache, key);
        if (l2Value != null) {
            safePut(l1Cache, key, l2Value.get());
        }
        return l2Value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        Object value = wrapper != null ? wrapper.get() : null;
        if (value == null) {
            return null;
        }
        if (type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]");
        }
        return type.cast(value);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        try (KeyLock.Locked ignored = keyLock.lock(key)) {
            wrapper = get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception ex) {
            throw new ValueRetrievalException(key, valueLoader, ex);
        }
    }

    @Override
    public void put(Object key, Object value) {
        safePut(l2Cache, key, value);
        safePut(l1Cache, key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        safePut(l2Cache, key, value);
        safePut(l1Cache, key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        safeEvict(l1Cache, key);
        safeEvict(l2Cache, key);
    }

    @Override
    public void clear() {
        safeClear(l1Cache);
        safeClear(l2Cache);
    }

    private ValueWrapper safeGet(Cache cache, Object key) {
        if (cache == null) {
            return null;
        }
        try {
            return cache.get(key);
        } catch (RuntimeException ex) {
            logger.warn("Cache get failed for {} key {}", name, key, ex);
            return null;
        }
    }

    private void safePut(Cache cache, Object key, Object value) {
        if (cache == null) {
            return;
        }
        try {
            cache.put(key, value);
        } catch (RuntimeException ex) {
            logger.warn("Cache put failed for {} key {}", name, key, ex);
        }
    }

    private void safeEvict(Cache cache, Object key) {
        if (cache == null) {
            return;
        }
        try {
            cache.evict(key);
        } catch (RuntimeException ex) {
            logger.warn("Cache evict failed for {} key {}", name, key, ex);
        }
    }

    private void safeClear(Cache cache) {
        if (cache == null) {
            return;
        }
        try {
            cache.clear();
        } catch (RuntimeException ex) {
            logger.warn("Cache clear failed for {}", name, ex);
        }
    }

    private Object nativeCache(Cache cache) {
        return cache != null ? cache.getNativeCache() : null;
    }
}
