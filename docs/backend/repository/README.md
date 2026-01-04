# 后端数据访问层 (Repository Layer)

## 📋 概述

Repository层是Spring Data JPA的核心，提供数据访问抽象。通过接口继承，自动获得CRUD操作和查询方法。

## 🏗️ 架构设计

### 继承关系
```
JpaRepository<T, ID>
    ↓
JpaRepositoryImpl<T, ID> (自动生成)
    ↓
自定义Repository接口
    ↓
Spring代理实现
```

### 核心接口
```java
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // 自定义查询方法
}
```

**自动获得的方法**:
- `save()`, `saveAll()`
- `findById()`, `existsById()`
- `findAll()`, `findAll(Sort)`, `findAll(Pageable)`
- `count()`, `delete()`, `deleteAll()`
- `flush()`, `saveAndFlush()`

## 📚 Repository详解

### 1. UserRepository

**文件**: `src/main/java/com/example/blog/repository/UserRepository.java`

**功能描述**: 用户数据访问接口。

**核心代码**:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据用户名查询
    Optional<User> findByUsername(String username);

    // 根据邮箱查询
    Optional<User> findByEmail(String email);

    // 检查用户名是否存在
    boolean existsByUsername(String username);

    // 检查邮箱是否存在
    boolean existsByEmail(String email);

    // 根据用户名删除
    void deleteByUsername(String username);
}
```

**查询方法命名规则**:
- `findBy` + 字段名 → 精确匹配
- `existsBy` + 字段名 → 存在性检查
- `deleteBy` + 字段名 → 删除操作

**使用示例**:
```java
// 基础查询
User user = userRepository.findByUsername("admin").orElse(null);

// 存在性检查
if (userRepository.existsByUsername(username)) {
    throw new BusinessException("用户名已存在");
}

// 分页查询
Page<User> users = userRepository.findAll(PageRequest.of(0, 10));
```

---

### 2. ArticleRepository

**文件**: `src/main/java/com/example/blog/repository/ArticleRepository.java`

**功能描述**: 文章数据访问接口，包含复杂查询。

**核心代码**:
```java
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 基础查询
    Optional<Article> findBySlug(String slug);

    Page<Article> findByStatus(Article.Status status, Pageable pageable);

    Page<Article> findByAuthor(User author, Pageable pageable);

    Page<Article> findByCategory(Category category, Pageable pageable);

    Page<Article> findByCategoryAndStatus(Category category, Article.Status status, Pageable pageable);

    Page<Article> findByTagsContaining(Tag tag, Pageable pageable);

    PageArticle> findByTagsContainingAndStatus(Tag tag, Article.Status status, Pageable pageable);

    // 自定义JPQL查询
    @Query("SELECT a FROM Article a WHERE a.status = :status ORDER BY a.isTop DESC, a.publishedAt DESC")
    Page<Article> findPublishedArticles(@Param("status") Article.Status status, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 更新操作
    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Article a SET a.likeCount = a.likeCount + 1 WHERE a.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    // 统计方法
    long countByStatus(Article.Status status);
    long countByAuthor(User author);
    long countByCategory(Category category);
}
```

**设计模式分析**:

#### 1. 方法命名查询
```java
// Spring Data JPA自动解析为SQL
Page<Article> findByStatusAndIsTop(Article.Status status, Boolean isTop);
// → SELECT * FROM articles WHERE status = ? AND is_top = ? ORDER BY ...
```

#### 2. 自定义JPQL查询
```java
@Query("SELECT a FROM Article a WHERE a.status = :status ORDER BY a.isTop DESC, a.publishedAt DESC")
```
**优势**:
- 复杂排序逻辑
- 性能优化
- 可读性更好

#### 3. 动态查询
```java
// 多条件组合查询
Page<Article> findByCategoryAndStatus(Category category, Article.Status status, Pageable pageable);
```
**自动支持**:
- AND/OR组合
- 分页参数
- 排序控制

#### 4. 更新查询
```java
@Modifying
@Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
```
**注意**:
- 必须添加`@Modifying`注解
- 返回值为int（影响行数）
- 需要`@Transactional`支持

**使用示例**:
```java
// 1. 基础查询
Article article = articleRepository.findBySlug("spring-boot-guide")
    .orElseThrow(() -> new BusinessException("文章不存在"));

// 2. 分页查询
Pageable pageable = PageRequest.of(0, 10, Sort.by("publishedAt").descending());
Page<Article> articles = articleRepository.findByStatus(Article.Status.PUBLISHED, pageable);

// 3. 复杂查询：分类+状态+分页
Page<Article> categoryArticles = articleRepository.findByCategoryAndStatus(
    category, Article.Status.PUBLISHED, PageRequest.of(0, 10)
);

// 4. 搜索
Page<Article> searchResults = articleRepository.searchByKeyword(
    "Spring Boot", PageRequest.of(0, 10)
);

// 5. 更新计数
articleRepository.incrementViewCount(articleId);

// 6. 统计
long publishedCount = articleRepository.countByStatus(Article.Status.PUBLISHED);
```

---

### 3. CommentRepository

**文件**: `src/main/java/com/example/blog/repository/CommentRepository.java`

**功能描述**: 评论数据访问接口，支持层级查询。

**核心代码**:
```java
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 文章评论查询
    Page<Comment> findByArticle(Article article, Pageable pageable);

    Page<Comment> findByArticleAndStatus(Article article, Comment.Status status, Pageable pageable);

    // 层级查询：查询文章的顶级评论（无父评论）
    List<Comment> findByArticleAndParentIsNullAndStatus(Article article, Comment.Status status);

    // 审核查询
    Page<Comment> findByStatus(Comment.Status status, Pageable pageable);

    // 统计
    long countByArticle(Article article);
    long countByArticleAndStatus(Article article, Comment.Status status);
    long countByStatus(Comment.Status status);
}
```

**查询策略**:

#### 1. 分页查询
```java
Page<Comment> comments = commentRepository.findByArticleAndStatus(
    article, Comment.Status.APPROVED, PageRequest.of(0, 20)
);
```

#### 2. 层级查询
```java
// 查询顶级评论（parent=null）
List<Comment> rootComments = commentRepository.findByArticleAndParentIsNullAndStatus(
    article, Comment.Status.APPROVED
);

// 在Service层手动构建树形结构
rootComments.forEach(comment -> {
    List<Comment> replies = commentRepository.findByParent(comment);
    comment.setReplies(replies);
});
```

#### 3. 审核查询
```java
// 后台管理：查询待审核评论
Page<Comment> pendingComments = commentRepository.findByStatus(
    Comment.Status.PENDING, PageRequest.of(0, 10)
);
```

---

### 4. TagRepository

**文件**: `src/main/java/com/example/blog/repository/TagRepository.java`

**功能描述**: 标签数据访问接口，包含批量查询。

**核心代码**:
```java
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    Optional<Tag> findBySlug(String slug);

    boolean existsByName(String name);

    // 批量查询
    Set<Tag> findByIdIn(Set<Long> ids);

    // 自定义查询：按文章数量排序
    @Query("SELECT t, COUNT(a) as articleCount " +
           "FROM Tag t JOIN t.articles a " +
           "GROUP BY t.id ORDER BY articleCount DESC")
    List<Object[]> findTagsWithArticleCount();
}
```

**特色功能**:

#### 批量查询优化
```java
// 一次性查询多个标签
Set<Tag> tags = tagRepository.findByIdIn(Set.of(1L, 2L, 3L));
// 等同于：WHERE id IN (1, 2, 3)
```

#### 关联统计
```java
// 查询标签及其文章数量
List<Object[]> results = tagRepository.findTagsWithArticleCount();
for (Object[] result : results) {
    Tag tag = (Tag) result[0];
    Long count = (Long) result[1];
    System.out.println(tag.getName() + ": " + count + "篇文章");
}
```

---

### 5. ArticleLikeRepository

**文件**: `src/main/java/com/example/blog/repository/ArticleLikeRepository.java`

**功能描述**: 点赞记录访问接口。

**核心代码**:
```java
@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    Optional<ArticleLike> findByArticleAndUser(Article article, User user);

    boolean existsByArticleAndUser(Article article, User user);

    void deleteByArticleAndUser(Article article, User user);

    long countByArticle(Article article);
}
```

**业务场景**:
```java
// 检查是否已点赞
boolean liked = articleLikeRepository.existsByArticleAndUser(article, user);

// 获取点赞数
long likeCount = articleLikeRepository.countByArticle(article);

// 点赞/取消点赞
Optional<ArticleLike> existing = articleLikeRepository.findByArticleAndUser(article, user);
if (existing.isPresent()) {
    articleLikeRepository.delete(existing.get());
} else {
    ArticleLike like = new ArticleLike();
    like.setArticle(article);
    like.setUser(user);
    articleLikeRepository.save(like);
}
```

---

### 6. CategoryRepository

**文件**: `src/main/java/com/example/blog/repository/CategoryRepository.java`

**功能描述**: 分类数据访问接口。

**核心代码**:
```java
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    // 按排序顺序查询
    List<Category> findAllByOrderBySortOrderAsc();

    // 自定义查询：分类文章统计
    @Query("SELECT c, COUNT(a) as articleCount " +
           "FROM Category c LEFT JOIN c.articles a " +
           "GROUP BY c.id ORDER BY c.sortOrder ASC")
    List<Object[]> findCategoriesWithArticleCount();
}
```

**排序查询**:
```java
// 按sortOrder字段排序
List<Category> categories = categoryRepository.findAllByOrderBySortOrderAsc();
```

---

## 🔍 查询方法命名规则

### 基本模式
```
findBy[Field] + [Operator] + [Logic]
```

### 支持的操作符
| 操作符 | 示例 | SQL |
|--------|------|-----|
| (无) | `findByUsername` | `WHERE username = ?` |
| Like | `findByTitleLike` | `WHERE title LIKE ?` |
| IgnoreCase | `findByUsernameIgnoreCase` | `WHERE username = ? (忽略大小写)` |
| And | `findByTitleAndStatus` | `WHERE title = ? AND status = ?` |
| Or | `findByTitleOrContent` | `WHERE title = ? OR content = ?` |
| In | `findByIdIn` | `WHERE id IN (?)` |
| IsNull | `findByCategoryIsNull` | `WHERE category IS NULL` |
| IsNotNull | `findByCategoryIsNotNull` | `WHERE category IS NOT NULL` |
| True/False | `findByIsTopTrue` | `WHERE is_top = true` |
| Before/After | `findByCreatedAtBefore` | `WHERE created_at < ?` |
| GreaterThan | `findByViewCountGreaterThan` | `WHERE view_count > ?` |

### 分页和排序
```java
// 分页
Page<Article> findByStatus(Article.Status status, Pageable pageable);

// 排序
List<Article> findByStatusOrderByPublishedAtDesc(Article.Status status);

// 组合
Page<Article> findByStatus(Article.Status status, PageRequest.of(0, 10, Sort.by("publishedAt").descending()));
```

## 🎯 性能优化

### 1. N+1查询问题解决

**问题**:
```java
// 错误：每访问一次author属性就查询一次
List<Article> articles = articleRepository.findAll();
for (Article article : articles) {
    System.out.println(article.getAuthor().getUsername()); // N+1查询
}
```

**解决方案**:
```java
// 方案1：使用@Query指定JOIN FETCH
@Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.status = :status")
Page<Article> findPublishedArticles(@Param("status") Article.Status status, Pageable pageable);

// 方案2：在Service层批量查询
List<Article> articles = articleRepository.findAll();
Set<Long> authorIds = articles.stream()
    .map(a -> a.getAuthor().getId())
    .collect(Collectors.toSet());
Map<Long, User> users = userRepository.findAllById(authorIds).stream()
    .collect(Collectors.toMap(User::getId, u -> u));
```

### 2. 分页优化

```java
// 使用Pageable避免全表查询
Pageable pageable = PageRequest.of(0, 10, Sort.by("publishedAt").descending());
Page<Article> page = articleRepository.findByStatus(Article.Status.PUBLISHED, pageable);

// 获取分页信息
int totalPages = page.getTotalPages();
long totalElements = page.getTotalElements();
List<Article> content = page.getContent();
```

### 3. 批量操作

```java
// 批量保存
List<Article> articles = ...;
articleRepository.saveAll(articles);

// 批量删除
articleRepository.deleteAllById(ids);
```

### 4. 查询缓存

```java
// Spring Data JPA支持查询缓存
@QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
@Query("SELECT a FROM Article a WHERE a.status = :status")
Page<Article> findCachedPublishedArticles(@Param("status") Article.Status status, Pageable pageable);
```

## 📊 事务管理

### 事务边界
```java
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    @Transactional  // 事务注解
    public ArticleDTO createArticle(ArticleRequest request, String username) {
        // 数据库操作
        Article article = new Article();
        // ... 设置属性
        articleRepository.save(article);

        // 如果抛出异常，自动回滚
        return ArticleDTO.fromEntity(article);
    }
}
```

### 事务传播行为
```java
// REQUIRED: 如果存在事务则加入，否则新建（默认）
@Transactional(propagation = Propagation.REQUIRED)

// REQUIRES_NEW: 总是新建事务
@Transactional(propagation = Propagation.REQUIRES_NEW)

// SUPPORTS: 如果存在事务则加入，否则非事务执行
@Transactional(propagation = Propagation.SUPPORTS)
```

## 🧪 测试Repository

### 单元测试
```java
@DataJpaTest
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void shouldFindArticleBySlug() {
        // Given
        Article article = new Article();
        article.setTitle("Test");
        article.setSlug("test");
        article.setContent("Content");
        article.setAuthor(user);
        articleRepository.save(article);

        // When
        Optional<Article> found = articleRepository.findBySlug("test");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test");
    }
}
```

## 📝 最佳实践

### 1. 命名规范
```java
// ✅ 推荐
findByUsername(String username)
findByArticleAndStatus(Article article, Comment.Status status)

// ❌ 避免
getByUserName(String username)  // 应该用findBy
findArticleById(Long id)       // findById已存在
```

### 2. 返回类型选择
```java
// 单个结果
Optional<User> findByUsername(String username);

// 多个结果
List<Article> findByStatus(Article.Status status);

// 分页
Page<Article> findByStatus(Article.Status status, Pageable pageable);

// 统计
long countByStatus(Article.Status status);

// 存在性
boolean existsByUsername(String username);
```

### 3. 自定义查询优先级
```java
// 简单查询：方法命名
Optional<Article> findBySlug(String slug);

// 复杂查询：@Query
@Query("SELECT a FROM Article a WHERE ...")
Page<Article> complexQuery(...);

// 非常复杂：原生SQL
@Query(value = "SELECT * FROM articles WHERE ...", nativeQuery = true)
List<Article> nativeQuery();
```

### 4. 避免在Repository中写业务逻辑
```java
// ❌ 错误
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // 不要在Repository中写业务逻辑
    default ArticleDTO getArticleWithLikeStatus(Long id, String username) {
        // ... 业务逻辑
    }
}

// ✅ 正确：业务逻辑在Service层
@Service
public class ArticleService {
    public ArticleDTO getArticleWithLikeStatus(Long id, String username) {
        Article article = articleRepository.findById(id).orElseThrow(...);
        boolean liked = checkLikeStatus(article, username);
        return ArticleDTO.fromEntity(article, liked);
    }
}
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队