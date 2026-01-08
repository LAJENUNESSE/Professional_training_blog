package com.example.blog.config;

import com.example.blog.cache.CacheNames;
import com.example.blog.cache.CacheProperties;
import com.example.blog.cache.JitterExpiry;
import com.example.blog.cache.MultiLevelCacheManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager(CacheProperties properties,
                                     ObjectMapper objectMapper,
                                     ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        if (!properties.isEnabled()) {
            return new NoOpCacheManager();
        }

        CaffeineCacheManager l1CacheManager = buildCaffeineCacheManager(properties);

        if (!properties.isRedisEnabled()) {
            return l1CacheManager;
        }

        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        if (connectionFactory == null) {
            logger.warn("RedisConnectionFactory not found, falling back to L1 cache only");
            return l1CacheManager;
        }

        RedisCacheManager l2CacheManager = buildRedisCacheManager(properties, objectMapper, connectionFactory);
        return new MultiLevelCacheManager(l1CacheManager, l2CacheManager);
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logger.warn("Cache get error for {} key {}", cache != null ? cache.getName() : "unknown", key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                logger.warn("Cache put error for {} key {}", cache != null ? cache.getName() : "unknown", key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                logger.warn("Cache evict error for {} key {}", cache != null ? cache.getName() : "unknown", key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                logger.warn("Cache clear error for {}", cache != null ? cache.getName() : "unknown", exception);
            }
        };
    }

    private CaffeineCacheManager buildCaffeineCacheManager(CacheProperties properties) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(properties.getL1MaxSize());
        if (properties.getL1Jitter() != null && !properties.getL1Jitter().isZero()) {
            caffeine.expireAfter(new JitterExpiry(
                    properties.getL1Ttl().toNanos(),
                    properties.getL1Jitter().toNanos()
            ));
        } else {
            caffeine.expireAfterWrite(properties.getL1Ttl());
        }

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAllowNullValues(true);
        cacheManager.setCacheNames(CacheNames.all());
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    private RedisCacheManager buildRedisCacheManager(CacheProperties properties,
                                                     ObjectMapper objectMapper,
                                                     RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CacheNames.PUBLISHED_ARTICLES, baseConfig.entryTtl(withJitter(properties.getPublishedTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.ARTICLES_BY_CATEGORY, baseConfig.entryTtl(withJitter(properties.getArticlesByCategoryTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.ARTICLES_BY_TAG, baseConfig.entryTtl(withJitter(properties.getArticlesByTagTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.HOT_ARTICLES, baseConfig.entryTtl(withJitter(properties.getHotTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.CATEGORY_LIST, baseConfig.entryTtl(withJitter(properties.getCategoryTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.TAG_LIST, baseConfig.entryTtl(withJitter(properties.getTagTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.CATEGORY_EXISTS, baseConfig.entryTtl(withJitter(properties.getExistsTtl(), properties.getL2Jitter())));
        cacheConfigs.put(CacheNames.TAG_EXISTS, baseConfig.entryTtl(withJitter(properties.getExistsTtl(), properties.getL2Jitter())));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private Duration withJitter(Duration base, Duration jitter) {
        if (base == null || jitter == null || jitter.isZero()) {
            return base;
        }
        long jitterMillis = Math.max(0L, jitter.toMillis());
        long extraMillis = ThreadLocalRandom.current().nextLong(jitterMillis + 1);
        return base.plusMillis(extraMillis);
    }
}
