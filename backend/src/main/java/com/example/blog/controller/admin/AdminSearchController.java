package com.example.blog.controller.admin;

import com.example.blog.common.Result;
import com.example.blog.search.ArticleSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final ArticleSearchService articleSearchService;

    @PostMapping("/reindex")
    public Result<Void> reindex() {
        articleSearchService.reindexAll();
        return Result.success();
    }
}
