package com.example.blog.cache;

import com.github.benmanes.caffeine.cache.Expiry;

import java.util.concurrent.ThreadLocalRandom;

public class JitterExpiry implements Expiry<Object, Object> {

    private final long baseNanos;
    private final long jitterNanos;

    public JitterExpiry(long baseNanos, long jitterNanos) {
        this.baseNanos = baseNanos;
        this.jitterNanos = jitterNanos;
    }

    @Override
    public long expireAfterCreate(Object key, Object value, long currentTime) {
        return baseNanos + randomJitter();
    }

    @Override
    public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
        return baseNanos + randomJitter();
    }

    @Override
    public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
        return currentDuration;
    }

    private long randomJitter() {
        if (jitterNanos <= 0) {
            return 0L;
        }
        return ThreadLocalRandom.current().nextLong(jitterNanos + 1);
    }
}
