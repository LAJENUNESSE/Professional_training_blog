package com.example.blog.cache;

import com.example.blog.service.ArticleService;
import com.example.blog.service.CategoryService;
import com.example.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
@RequiredArgsConstructor
public class CacheWarmupRunner {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final CacheProperties cacheProperties;
    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupCaches() {
        if (!cacheProperties.isEnabled() || !cacheProperties.isWarmupEnabled()) {
            return;
        }
        try {
            articleService.getPublishedArticles(PageRequest.of(0, cacheProperties.getWarmupPageSize()));
            int hotSize = Math.min(cacheProperties.getWarmupHotSize(), cacheProperties.getMaxHotSize());
            articleService.getHotArticles(hotSize);
            categoryService.getAllCategories();
            tagService.getAllTags();
            logger.info("Cache warmup completed");
        } catch (Exception ex) {
            logger.warn("Cache warmup failed", ex);
        }
    }
}
