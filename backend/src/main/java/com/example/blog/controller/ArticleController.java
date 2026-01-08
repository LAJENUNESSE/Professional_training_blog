package com.example.blog.controller;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import com.example.blog.dto.response.ArticleDTO;
import com.example.blog.entity.Article;
import com.example.blog.exception.BusinessException;
import com.example.blog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Result<PageResult<ArticleDTO>> getPublishedArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.getPublishedArticles(pageable)));
    }

    @GetMapping("/hot")
    public Result<List<ArticleDTO>> getHotArticles(
            @RequestParam(defaultValue = "10") int size) {
        int resolvedSize = Math.max(1, size);
        return Result.success(articleService.getHotArticles(resolvedSize));
    }

    @GetMapping("/{id}")
    public Result<ArticleDTO> getArticleById(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ArticleDTO article = articleService.getArticleById(id, username);
        if (article.getStatus().equals(Article.Status.PUBLISHED.name())) {
            articleService.incrementViewCount(id);
        }
        return Result.success(article);
    }

    @GetMapping("/slug/{slug}")
    public Result<ArticleDTO> getArticleBySlug(@PathVariable String slug, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ArticleDTO article = articleService.getArticleBySlug(slug, username);
        if (article.getStatus().equals(Article.Status.PUBLISHED.name())) {
            articleService.incrementViewCount(article.getId());
        }
        return Result.success(article);
    }

    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ArticleDTO>> getArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.getPublishedArticlesByCategory(categoryId, pageable)));
    }

    @GetMapping("/tag/{tagId}")
    public Result<PageResult<ArticleDTO>> getArticlesByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.getPublishedArticlesByTag(tagId, pageable)));
    }

    @GetMapping("/search")
    public Result<PageResult<ArticleDTO>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.searchArticles(keyword, pageable)));
    }

    @PostMapping("/{id}/like")
    public Result<ArticleService.LikeResult> likeArticle(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            throw BusinessException.unauthorized("Unauthorized");
        }
        return Result.success(articleService.toggleLike(id, username));
    }
}
