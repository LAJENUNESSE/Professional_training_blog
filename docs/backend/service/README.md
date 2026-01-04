# 后端业务逻辑层 (Service Layer)

## 📋 概述

Service层是业务逻辑的核心，负责处理业务规则、数据验证、事务管理和跨多个Repository的协调操作。

## 🏗️ 架构设计

### 分层架构
```
Controller (HTTP请求/响应)
    ↓
Service (业务逻辑)
    ↓
Repository (数据访问)
    ↓
Entity (数据库映射)
```

### 事务边界
```java
@Service
@RequiredArgsConstructor
public class ArticleService {

    @Transactional  // 事务注解
    public ArticleDTO createArticle(...) {
        // 多个数据库操作在一个事务中
        articleRepository.save(article);
        tagRepository.saveAll(tags);
        // 如果异常，全部回滚
    }
}
```

## 📚 Service详解

### 1. AuthService (认证服务)

**文件**: `src/main/java/com/example/blog/service/AuthService.java`

**功能描述**: 处理用户注册、登录、Token刷新等认证相关业务。

**核心代码**:
```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. 验证用户名和邮箱唯一性
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.badRequest("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.badRequest("Email already exists");
        }

        // 2. 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt加密
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        // 3. 保存用户
        userRepository.save(user);

        // 4. 生成JWT Token
        String accessToken = jwtTokenProvider.generateToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 5. 返回响应
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(AuthResponse.UserInfo.fromUser(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Spring Security认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 2. 查询用户并验证状态
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (!user.getEnabled()) {
            throw BusinessException.forbidden("User is disabled");
        }

        // 3. 生成Token
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(AuthResponse.UserInfo.fromUser(user))
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        // 1. 验证Token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw BusinessException.unauthorized("Invalid refresh token");
        }

        // 2. 提取用户名
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // 3. 生成新Token
        String newAccessToken = jwtTokenProvider.generateToken(username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getExpiration())
                .user(AuthResponse.UserInfo.fromUser(user))
                .build();
    }
}
```

**业务流程**:

#### 注册流程
```
输入验证 → 唯一性检查 → 密码加密 → 创建用户 → 生成Token → 返回响应
```

#### 登录流程
```
用户名密码 → Spring Security认证 → 查询用户 → 验证状态 → 生成Token → 返回响应
```

#### Token刷新流程
```
Refresh Token → 验证有效性 → 提取用户名 → 生成新Token → 返回响应
```

**输入输出**:

| 方法 | 输入 | 输出 | 异常 |
|------|------|------|------|
| register | RegisterRequest | AuthResponse | 用户名/邮箱已存在 |
| login | LoginRequest | AuthResponse | 用户不存在/禁用 |
| refreshToken | refreshToken | AuthResponse | Token无效 |

**依赖关系**:
- UserRepository: 用户数据访问
- PasswordEncoder: 密码加密
- JwtTokenProvider: Token生成
- AuthenticationManager: Spring Security认证
- JwtConfig: JWT配置

---

### 2. ArticleService (文章服务)

**文件**: `src/main/java/com/example/blog/service/ArticleService.java`

**功能描述**: 处理文章的增删改查、点赞、统计等业务。

**核心代码**:
```java
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    // 查询已发布文章（分页）
    public Page<ArticleDTO> getPublishedArticles(Pageable pageable) {
        return articleRepository.findPublishedArticles(Article.Status.PUBLISHED, pageable)
                .map(ArticleDTO::fromEntityList);
    }

    // 根据ID查询文章（带点赞状态）
    @Transactional(readOnly = true)
    public ArticleDTO getArticleById(Long id, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));
        Boolean liked = resolveLiked(article, username);
        return ArticleDTO.fromEntity(article, liked);
    }

    // 创建文章
    @Transactional
    public ArticleDTO createArticle(ArticleRequest request, String username) {
        // 1. 验证作者
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // 2. 创建文章实体
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle()));
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setStatus(Article.Status.valueOf(request.getStatus()));
        article.setIsTop(request.getIsTop());
        article.setAllowComment(request.getAllowComment());
        article.setAuthor(author);

        // 3. 设置分类
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BusinessException.notFound("Category not found"));
            article.setCategory(category);
        }

        // 4. 设置标签
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            article.setTags(tags);
        }

        // 5. 设置发布时间
        if (article.getStatus() == Article.Status.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now());
        }

        // 6. 保存
        articleRepository.save(article);
        return ArticleDTO.fromEntity(article);
    }

    // 更新文章
    @Transactional
    public ArticleDTO updateArticle(Long id, ArticleRequest request, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));

        // 权限验证：作者或管理员
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (!article.getAuthor().getId().equals(currentUser.getId())
            && currentUser.getRole() != User.Role.ADMIN) {
            throw BusinessException.forbidden("You don't have permission to edit this article");
        }

        // 更新字段
        article.setTitle(request.getTitle());
        article.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle()));
        // ... 其他字段更新

        articleRepository.save(article);
        return ArticleDTO.fromEntity(article);
    }

    // 点赞/取消点赞
    @Transactional
    public LikeResult toggleLike(Long id, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        Optional<ArticleLike> existing = articleLikeRepository.findByArticleAndUser(article, user);
        boolean liked;

        if (existing.isPresent()) {
            // 取消点赞
            articleLikeRepository.delete(existing.get());
            article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
            liked = false;
        } else {
            // 点赞
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticle(article);
            articleLike.setUser(user);
            articleLikeRepository.save(articleLike);
            article.setLikeCount(article.getLikeCount() + 1);
            liked = true;
        }

        articleRepository.save(article);
        return new LikeResult(article.getLikeCount(), liked);
    }

    // 生成Slug（URL友好）
    private String generateSlug(String title) {
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) {
            slug = "article-" + System.currentTimeMillis();
        }
        return slug;
    }

    // 解析用户是否点赞
    private Boolean resolveLiked(Article article, String username) {
        if (username == null) return null;
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;
        return articleLikeRepository.existsByArticleAndUser(article, user);
    }

    public record LikeResult(int likeCount, boolean liked) {}
}
```

**业务逻辑详解**:

#### 1. 权限控制
```java
// 只有作者或管理员可以编辑
if (!article.getAuthor().getId().equals(currentUser.getId())
    && currentUser.getRole() != User.Role.ADMIN) {
    throw BusinessException.forbidden("无权限");
}
```

#### 2. 点赞防重
```java
// 使用唯一约束 + 事务保证原子性
Optional<ArticleLike> existing = articleLikeRepository.findByArticleAndUser(article, user);
if (existing.isPresent()) {
    // 已点赞 → 取消
    articleLikeRepository.delete(existing.get());
    article.setLikeCount(article.getLikeCount() - 1);
} else {
    // 未点赞 → 点赞
    articleLikeRepository.save(newLike);
    article.setLikeCount(article.getLikeCount() + 1);
}
```

#### 3. Slug生成策略
```java
// 标题 → URL友好字符串
"Spring Boot 教程" → "spring-boot-教程" → "spring-boot-"
// 如果纯中文 → 使用时间戳
"文章标题" → "article-1234567890"
```

**使用示例**:
```java
// 1. 获取文章列表
Pageable pageable = PageRequest.of(0, 10, Sort.by("publishedAt").descending());
Page<ArticleDTO> articles = articleService.getPublishedArticles(pageable);

// 2. 获取单篇文章
ArticleDTO article = articleService.getArticleById(1L, "username");

// 3. 创建文章
ArticleRequest request = new ArticleRequest();
request.setTitle("新文章");
request.setContent("内容");
request.setStatus("PUBLISHED");
ArticleDTO created = articleService.createArticle(request, "author");

// 4. 点赞
LikeResult result = articleService.toggleLike(1L, "username");
System.out.println("点赞数: " + result.likeCount() + ", 是否点赞: " + result.liked());
```

---

### 3. CommentService (评论服务)

**文件**: `src/main/java/com/example/blog/service/CommentService.java`

**功能描述**: 处理评论的创建、审核、查询等业务。

**核心代码**:
```java
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    // 获取文章的已审核评论（树形结构）
    public List<CommentDTO> getApprovedCommentsByArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));

        // 查询顶级评论（parent=null）
        return commentRepository.findByArticleAndParentIsNullAndStatus(article, Comment.Status.APPROVED)
                .stream()
                .map(CommentDTO::fromEntityWithReplies)
                .collect(Collectors.toList());
    }

    // 创建评论
    @Transactional
    public CommentDTO createComment(Long articleId, CommentRequest request,
                                   String username, String ipAddress, String userAgent) {
        // 1. 验证文章
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));

        // 2. 检查是否允许评论
        if (!article.getAllowComment()) {
            throw BusinessException.badRequest("Comments are not allowed for this article");
        }

        // 3. 创建评论
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setArticle(article);
        comment.setIpAddress(ipAddress);
        comment.setUserAgent(userAgent);

        // 4. 用户身份处理
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                comment.setUser(user);
                comment.setStatus(Comment.Status.APPROVED); // 登录用户自动通过
            }
        }

        // 5. 匿名评论处理
        if (comment.getUser() == null) {
            comment.setAuthorName(request.getAuthorName());
            comment.setAuthorEmail(request.getAuthorEmail());
            comment.setAuthorUrl(request.getAuthorUrl());
            comment.setStatus(Comment.Status.PENDING); // 需审核
        }

        // 6. 回复处理
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> BusinessException.notFound("Parent comment not found"));

            // 验证父评论属于当前文章
            if (!parent.getArticle().getId().equals(articleId)) {
                throw BusinessException.badRequest("Parent comment does not belong to this article");
            }
            comment.setParent(parent);
        }

        commentRepository.save(comment);
        return CommentDTO.fromEntity(comment);
    }

    // 审核评论
    @Transactional
    public CommentDTO approveComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Comment not found"));
        comment.setStatus(Comment.Status.APPROVED);
        commentRepository.save(comment);
        return CommentDTO.fromEntity(comment);
    }

    // 拒绝评论
    @Transactional
    public CommentDTO rejectComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Comment not found"));
        comment.setStatus(Comment.Status.REJECTED);
        commentRepository.save(comment);
        return CommentDTO.fromEntity(comment);
    }

    // 获取待审核评论数
    public long getPendingCommentCount() {
        return commentRepository.countByStatus(Comment.Status.PENDING);
    }
}
```

**业务流程**:

#### 评论创建流程
```
验证文章 → 检查允许评论 → 创建实体 → 身份判断 → 审核状态 → 回复关联 → 保存
```

#### 审核流程
```
查询评论 → 验证存在 → 更新状态 → 保存 → 返回DTO
```

**权限策略**:
- **登录用户**: 评论自动APPROVED
- **匿名用户**: 评论状态PENDING，需管理员审核
- **回复**: 必须属于同一文章

---

### 4. FileStorageService (文件存储服务)

**文件**: `src/main/java/com/example/blog/service/FileStorageService.java`

**功能描述**: 处理文件上传、验证、存储。

**核心代码**:
```java
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public UploadResponse storeImage(MultipartFile file) {
        validateImage(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = generateFilename(extension);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

        try {
            Path targetDir = Paths.get(uploadDir, datePath);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String url = baseUrl + "/" + datePath + "/" + filename;

            return UploadResponse.of(
                    url,
                    filename,
                    originalFilename,
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw BusinessException.badRequest("Failed to store file: " + e.getMessage());
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("File size exceeds maximum allowed size (10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("Only image files are allowed");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String generateFilename(String extension) {
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
```

**文件上传流程**:
```
验证文件 → 检查大小和类型 → 生成文件名 → 创建日期目录 → 保存文件 → 返回URL
```

**安全措施**:
1. **类型白名单**: 只允许图片格式
2. **大小限制**: 10MB上限
3. **随机文件名**: UUID防止冲突和路径遍历
4. **日期目录**: 按年月组织，避免单一目录文件过多

---

## 🎯 设计模式

### 1. 事务管理
```java
@Transactional  // 方法级别事务
public class ArticleService {
    @Transactional(readOnly = true)  // 只读优化
    public ArticleDTO getArticle(...) { ... }

    @Transactional  // 读写事务
    public ArticleDTO createArticle(...) { ... }
}
```

### 2. 异常处理
```java
// 统一异常抛出
throw BusinessException.notFound("Article not found");
throw BusinessException.badRequest("Invalid input");
throw BusinessException.forbidden("No permission");
```

### 3. DTO转换
```java
// Repository → Entity → DTO
return articleRepository.findAll(pageable)
    .map(ArticleDTO::fromEntity);
```

### 4. 构建器模式
```java
return AuthResponse.builder()
    .accessToken(accessToken)
    .refreshToken(refreshToken)
    .user(userInfo)
    .build();
```

### 5. 策略模式
```java
// 根据用户类型设置不同状态
if (username != null) {
    comment.setStatus(Comment.Status.APPROVED);  // 登录用户
} else {
    comment.setStatus(Comment.Status.PENDING);   // 匿名用户
}
```

## 📊 事务传播示例

### 1. 只读事务
```java
@Transactional(readOnly = true)
public Page<ArticleDTO> getPublishedArticles(Pageable pageable) {
    // 优化：不开启写事务，提高性能
    return articleRepository.findPublishedArticles(...);
}
```

### 2. 读写事务
```java
@Transactional
public ArticleDTO createArticle(...) {
    // 多个写操作，原子性保证
    articleRepository.save(article);
    tagRepository.saveAll(tags);
    // 任一失败全部回滚
}
```

### 3. 事务传播行为
```java
// REQUIRES_NEW: 独立事务
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logAudit(Long articleId) {
    // 审计日志独立于主事务
}
```

## 🔍 业务规则验证

### 1. 唯一性验证
```java
if (userRepository.existsByUsername(request.getUsername())) {
    throw BusinessException.badRequest("用户名已存在");
}
```

### 2. 权限验证
```java
if (!article.getAuthor().getId().equals(currentUser.getId())
    && currentUser.getRole() != User.Role.ADMIN) {
    throw BusinessException.forbidden("无权限");
}
```

### 3. 依赖验证
```java
Article article = articleRepository.findById(id)
    .orElseThrow(() -> BusinessException.notFound("文章不存在"));
```

### 4. 状态验证
```java
if (!article.getAllowComment()) {
    throw BusinessException.badRequest("文章不允许评论");
}
```

## 📈 性能优化

### 1. 懒加载优化
```java
// 避免N+1查询
@Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.status = :status")
Page<Article> findPublishedArticles(@Param("status") Article.Status status, Pageable pageable);
```

### 2. 批量操作
```java
// 批量保存标签
Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
article.setTags(tags);
```

### 3. 只读查询
```java
@Transactional(readOnly = true)
public ArticleDTO getArticleById(Long id, String username) {
    // 不开启写事务，提高性能
}
```

### 4. 缓存友好
```java
// 不变的数据可以缓存
private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(...);
```

## 🧪 测试策略

### 单元测试
```java
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void shouldCreateArticleSuccessfully() {
        // Given
        User author = new User();
        author.setUsername("author");

        ArticleRequest request = new ArticleRequest();
        request.setTitle("Test");
        request.setContent("Content");

        when(userRepository.findByUsername("author")).thenReturn(Optional.of(author));
        when(articleRepository.save(any(Article.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        ArticleDTO result = articleService.createArticle(request, "author");

        // Then
        assertThat(result.getTitle()).isEqualTo("Test");
        verify(articleRepository).save(any(Article.class));
    }
}
```

### 集成测试
```java
@SpringBootTest
@Transactional
class ArticleServiceIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Test
    void shouldHandleLikeTransaction() {
        // 测试点赞事务的原子性
        LikeResult result1 = articleService.toggleLike(1L, "user1");
        LikeResult result2 = articleService.toggleLike(1L, "user1");

        assertThat(result1.liked()).isTrue();
        assertThat(result2.liked()).isFalse();
    }
}
```

## 📝 最佳实践

### 1. 事务边界清晰
```java
// ✅ 推荐：在Service层加事务
@Service
public class ArticleService {
    @Transactional
    public void create(...) { ... }
}

// ❌ 避免：在Controller层加事务
@Controller
public class ArticleController {
    @Transactional  // 事务边界不清晰
    public void create(...) { ... }
}
```

### 2. 异常处理
```java
// ✅ 推荐：业务异常
throw BusinessException.badRequest("用户名已存在");

// ❌ 避免：通用异常
throw new RuntimeException("Error");
```

### 3. 单一职责
```java
// ✅ 推荐：Service专注业务逻辑
public class ArticleService {
    // 只负责文章业务
}

// ❌ 避免：Service混杂其他职责
public class ArticleService {
    // 不应该包含文件操作、邮件发送等
}
```

### 4. 依赖注入
```java
// ✅ 推荐：构造函数注入
@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
}

// ❌ 避免：字段注入
@Service
public class ArticleService {
    @Autowired
    private ArticleRepository articleRepository;
}
```

### 5. 命名规范
```java
// ✅ 推荐：清晰的业务意图
getPublishedArticles()
createArticle()
toggleLike()
approveComment()

// ❌ 避免：模糊的命名
getArticles()  // 哪些文章？
process()      // 做什么？
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队