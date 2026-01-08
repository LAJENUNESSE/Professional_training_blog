package com.example.blog.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("cacheProperties")
@ConfigurationProperties(prefix = "blog.cache")
public class CacheProperties {

    private boolean enabled = true;
    private boolean redisEnabled = true;
    private boolean warmupEnabled = true;
    private int maxCachedPages = 3;
    private int warmupPageSize = 10;
    private int warmupHotSize = 10;
    private int maxHotSize = 20;
    private int l1MaxSize = 500;
    private Duration l1Ttl = Duration.ofSeconds(30);
    private Duration l1Jitter = Duration.ofSeconds(10);
    private Duration publishedTtl = Duration.ofMinutes(5);
    private Duration articlesByCategoryTtl = Duration.ofMinutes(5);
    private Duration articlesByTagTtl = Duration.ofMinutes(5);
    private Duration hotTtl = Duration.ofMinutes(1);
    private Duration categoryTtl = Duration.ofMinutes(30);
    private Duration tagTtl = Duration.ofMinutes(30);
    private Duration existsTtl = Duration.ofMinutes(5);
    private Duration l2Jitter = Duration.ofSeconds(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public boolean isWarmupEnabled() {
        return warmupEnabled;
    }

    public void setWarmupEnabled(boolean warmupEnabled) {
        this.warmupEnabled = warmupEnabled;
    }

    public int getMaxCachedPages() {
        return maxCachedPages;
    }

    public void setMaxCachedPages(int maxCachedPages) {
        this.maxCachedPages = maxCachedPages;
    }

    public int getWarmupPageSize() {
        return warmupPageSize;
    }

    public void setWarmupPageSize(int warmupPageSize) {
        this.warmupPageSize = warmupPageSize;
    }

    public int getWarmupHotSize() {
        return warmupHotSize;
    }

    public void setWarmupHotSize(int warmupHotSize) {
        this.warmupHotSize = warmupHotSize;
    }

    public int getMaxHotSize() {
        return maxHotSize;
    }

    public void setMaxHotSize(int maxHotSize) {
        this.maxHotSize = maxHotSize;
    }

    public int getL1MaxSize() {
        return l1MaxSize;
    }

    public void setL1MaxSize(int l1MaxSize) {
        this.l1MaxSize = l1MaxSize;
    }

    public Duration getL1Ttl() {
        return l1Ttl;
    }

    public void setL1Ttl(Duration l1Ttl) {
        this.l1Ttl = l1Ttl;
    }

    public Duration getL1Jitter() {
        return l1Jitter;
    }

    public void setL1Jitter(Duration l1Jitter) {
        this.l1Jitter = l1Jitter;
    }

    public Duration getPublishedTtl() {
        return publishedTtl;
    }

    public void setPublishedTtl(Duration publishedTtl) {
        this.publishedTtl = publishedTtl;
    }

    public Duration getArticlesByCategoryTtl() {
        return articlesByCategoryTtl;
    }

    public void setArticlesByCategoryTtl(Duration articlesByCategoryTtl) {
        this.articlesByCategoryTtl = articlesByCategoryTtl;
    }

    public Duration getArticlesByTagTtl() {
        return articlesByTagTtl;
    }

    public void setArticlesByTagTtl(Duration articlesByTagTtl) {
        this.articlesByTagTtl = articlesByTagTtl;
    }

    public Duration getHotTtl() {
        return hotTtl;
    }

    public void setHotTtl(Duration hotTtl) {
        this.hotTtl = hotTtl;
    }

    public Duration getCategoryTtl() {
        return categoryTtl;
    }

    public void setCategoryTtl(Duration categoryTtl) {
        this.categoryTtl = categoryTtl;
    }

    public Duration getTagTtl() {
        return tagTtl;
    }

    public void setTagTtl(Duration tagTtl) {
        this.tagTtl = tagTtl;
    }

    public Duration getExistsTtl() {
        return existsTtl;
    }

    public void setExistsTtl(Duration existsTtl) {
        this.existsTtl = existsTtl;
    }

    public Duration getL2Jitter() {
        return l2Jitter;
    }

    public void setL2Jitter(Duration l2Jitter) {
        this.l2Jitter = l2Jitter;
    }
}
