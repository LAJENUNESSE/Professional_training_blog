package com.example.blog.controller.admin;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import com.example.blog.dto.request.ArticleRequest;
import com.example.blog.dto.response.ArticleDTO;
import com.example.blog.entity.Article;
import com.example.blog.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Result<PageResult<ArticleDTO>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Article.Status parsedStatus = status != null && !status.isEmpty() ? Article.Status.valueOf(status) : null;

        return Result.success(PageResult.of(
                articleService.searchArticles(parsedStatus, categoryId, tagId, keyword, pageable)
        ));
    }

    @GetMapping("/{id}")
    public Result<ArticleDTO> getArticleById(@PathVariable Long id) {
        return Result.success(articleService.getArticleById(id));
    }

    @PostMapping
    public Result<ArticleDTO> createArticle(
            @Valid @RequestBody ArticleRequest request,
            Authentication authentication) {
        return Result.success(articleService.createArticle(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public Result<ArticleDTO> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request,
            Authentication authentication) {
        return Result.success(articleService.updateArticle(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id, Authentication authentication) {
        articleService.deleteArticle(id, authentication.getName());
        return Result.success();
    }
}
