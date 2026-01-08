package com.example.blog.repository;

import com.example.blog.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);

    Set<Tag> findByIdIn(Set<Long> ids);

    @Query("SELECT t as tag, COUNT(a) as articleCount " +
           "FROM Tag t LEFT JOIN t.articles a " +
           "GROUP BY t ORDER BY t.name ASC")
    List<TagWithCount> findAllWithArticleCount();
}
