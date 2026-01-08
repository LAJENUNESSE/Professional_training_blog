package com.example.blog.repository;

import com.example.blog.entity.Category;

public interface CategoryWithCount {

    Category getCategory();

    long getArticleCount();
}
