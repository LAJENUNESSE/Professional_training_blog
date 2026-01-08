package com.example.blog.repository;

import com.example.blog.entity.Article;
import com.example.blog.entity.Category;
import com.example.blog.entity.Tag;
import com.example.blog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Article> findBySlug(String slug);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Article> findById(Long id);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findByIdInAndStatus(Collection<Long> ids, Article.Status status);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByStatus(Article.Status status, Pageable pageable);

    @Query(
            value = "SELECT DISTINCT a FROM Article a " +
                    "LEFT JOIN FETCH a.author " +
                    "LEFT JOIN FETCH a.category " +
                    "LEFT JOIN FETCH a.tags " +
                    "WHERE a.status = :status",
            countQuery = "SELECT COUNT(a) FROM Article a WHERE a.status = :status"
    )
    Page<Article> findByStatusWithTags(@Param("status") Article.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByAuthor(User author, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByCategory(Category category, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByCategoryAndStatus(Category category, Article.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByTagsContaining(Tag tag, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findByTagsContainingAndStatus(Tag tag, Article.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Page<Article> findAll(Specification<Article> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("SELECT a FROM Article a WHERE a.status = :status ORDER BY a.isTop DESC, a.publishedAt DESC")
    Page<Article> findPublishedArticles(@Param("status") Article.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' ORDER BY a.viewCount DESC, a.publishedAt DESC")
    Page<Article> findHotPublishedArticles(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    long countByStatus(Article.Status status);

    long countByAuthor(User author);

    long countByCategory(Category category);
}
