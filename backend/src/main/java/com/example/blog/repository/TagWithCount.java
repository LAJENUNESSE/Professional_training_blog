package com.example.blog.repository;

import com.example.blog.entity.Tag;

public interface TagWithCount {

    Tag getTag();

    long getArticleCount();
}
