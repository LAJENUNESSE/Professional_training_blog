package com.example.blog.repository;

import com.example.blog.entity.Article;
import com.example.blog.entity.Tag;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class ArticleSpecifications {

    private ArticleSpecifications() {
    }

    public static Specification<Article> withFilters(Article.Status status,
                                                     Long categoryId,
                                                     Long tagId,
                                                     String keyword) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (categoryId != null) {
                predicates.add(builder.equal(root.get("category").get("id"), categoryId));
            }

            if (tagId != null) {
                Join<Article, Tag> tagJoin = root.join("tags", JoinType.LEFT);
                predicates.add(builder.equal(tagJoin.get("id"), tagId));
                query.distinct(true);
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                Predicate titleLike = builder.like(builder.lower(root.get("title")), like);
                Predicate contentLike = builder.like(builder.lower(root.get("content")), like);
                predicates.add(builder.or(titleLike, contentLike));
            }

            return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
