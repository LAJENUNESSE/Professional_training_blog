package com.example.blog.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class KeyLock {

    private final ConcurrentHashMap<Object, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Locked lock(Object key) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
        return () -> {
            try {
                lock.unlock();
            } finally {
                if (!lock.hasQueuedThreads()) {
                    locks.remove(key, lock);
                }
            }
        };
    }

    public interface Locked extends AutoCloseable {
        @Override
        void close();
    }
}
