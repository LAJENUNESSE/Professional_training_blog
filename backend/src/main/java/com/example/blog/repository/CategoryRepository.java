package com.example.blog.repository;

import com.example.blog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    List<Category> findAllByOrderBySortOrderAsc();

    @Query("SELECT c as category, COUNT(a) as articleCount " +
           "FROM Category c LEFT JOIN c.articles a " +
           "GROUP BY c ORDER BY c.sortOrder ASC")
    List<CategoryWithCount> findAllWithArticleCount();
}
