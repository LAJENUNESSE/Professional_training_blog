# 后端控制器层 (Controller Layer)

## 📋 概述

Controller层是REST API的入口点，负责处理HTTP请求、参数验证、调用Service层业务逻辑，并返回统一格式的响应。

## 🏗️ 架构设计

### 请求处理流程
```
HTTP Request
    ↓
Controller (参数解析、验证)
    ↓
Service (业务逻辑)
    ↓
Repository (数据访问)
    ↓
Entity (数据库)
    ↓
DTO (数据转换)
    ↓
Controller (封装响应)
    ↓
HTTP Response (JSON)
```

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 📚 Controller详解

### 1. AuthController (认证控制器)

**文件**: `src/main/java/com/example/blog/controller/AuthController.java`

**功能描述**: 处理用户注册、登录、Token刷新等认证相关请求。

**核心代码**:
```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        return Result.success(authService.refreshToken(refreshToken));
    }
}
```

**路由映射**:
| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/auth/register` | 用户注册 | ❌ 公开 |
| POST | `/api/auth/login` | 用户登录 | ❌ 公开 |
| POST | `/api/auth/refresh` | Token刷新 | ❌ 公开 |

**请求示例**:

#### 注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "nickname": "新用户"
}

// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "newuser",
      "email": "user@example.com",
      "nickname": "新用户",
      "role": "USER"
    }
  }
}
```

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "nickname": "管理员",
      "role": "ADMIN"
    }
  }
}
```

#### 刷新Token
```http
POST /api/auth/refresh?refreshToken=eyJhbGciOiJIUzI1NiJ9...

// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "新eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "新eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": { ... }
  }
}
```

**参数验证**:
```java
// RegisterRequest
@NotBlank(message = "用户名不能为空")
@Size(min = 3, max = 20, message = "用户名长度3-20字符")
private String username;

@NotBlank(message = "密码不能为空")
@Size(min = 6, max = 50, message = "密码长度6-50字符")
private String password;

@Email(message = "邮箱格式不正确")
@NotBlank(message = "邮箱不能为空")
private String email;
```

---

### 2. ArticleController (文章控制器)

**文件**: `src/main/java/com/example/blog/controller/ArticleController.java`

**功能描述**: 处理文章的查询、搜索、点赞等请求。

**核心代码**:
```java
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // 获取已发布文章列表（分页）
    @GetMapping
    public Result<PageResult<ArticleDTO>> getPublishedArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.getPublishedArticles(pageable)));
    }

    // 根据ID获取文章
    @GetMapping("/{id}")
    public Result<ArticleDTO> getArticleById(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ArticleDTO article = articleService.getArticleById(id, username);

        // 增加浏览量（仅已发布文章）
        if (article.getStatus().equals(Article.Status.PUBLISHED.name())) {
            articleService.incrementViewCount(id);
        }
        return Result.success(article);
    }

    // 根据Slug获取文章
    @GetMapping("/slug/{slug}")
    public Result<ArticleDTO> getArticleBySlug(@PathVariable String slug, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ArticleDTO article = articleService.getArticleBySlug(slug, username);

        if (article.getStatus().equals(Article.Status.PUBLISHED.name())) {
            articleService.incrementViewCount(article.getId());
        }
        return Result.success(article);
    }

    // 按分类查询
    @GetMapping("/category/{categoryId}")
    public Result<PageResult<ArticleDTO>> getArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.getArticlesByCategory(categoryId, pageable)));
    }

    // 按标签查询
    @GetMapping("/tag/{tagId}")
    public Result<PageResult<ArticleDTO>> getArticlesByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.getArticlesByTag(tagId, pageable)));
    }

    // 搜索
    @GetMapping("/search")
    public Result<PageResult<ArticleDTO>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return Result.success(PageResult.of(articleService.searchArticles(keyword, pageable)));
    }

    // 点赞
    @PostMapping("/{id}/like")
    public Result<ArticleService.LikeResult> likeArticle(@PathVariable Long id, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            throw BusinessException.unauthorized("Unauthorized");
        }
        return Result.success(articleService.toggleLike(id, username));
    }
}
```

**路由映射**:
| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/articles` | 文章列表 | ❌ 公开 |
| GET | `/api/articles/{id}` | 文章详情 | ❌ 公开 |
| GET | `/api/articles/slug/{slug}` | 文章详情(Slug) | ❌ 公开 |
| GET | `/api/articles/category/{id}` | 分类文章 | ❌ 公开 |
| GET | `/api/articles/tag/{id}` | 标签文章 | ❌ 公开 |
| GET | `/api/articles/search` | 搜索文章 | ❌ 公开 |
| POST | `/api/articles/{id}/like` | 点赞 | ✅ 需要登录 |

**分页参数**:
- `page`: 页码，默认0（从0开始）
- `size`: 每页数量，默认10
- 排序：按发布时间降序（publishedAt DESC）

**查询示例**:

#### 分页查询
```http
GET /api/articles?page=0&size=10

// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Spring Boot 教程",
        "slug": "spring-boot-guide",
        "summary": "入门指南...",
        "content": "...",
        "coverImage": "/uploads/2024/01/abc.jpg",
        "status": "PUBLISHED",
        "viewCount": 100,
        "likeCount": 10,
        "publishedAt": "2024-01-01T10:00:00",
        "author": { "id": 1, "username": "admin" },
        "category": { "id": 1, "name": "技术" },
        "tags": [{ "id": 1, "name": "Java" }],
        "liked": true  // 当前用户是否点赞
      }
    ],
    "page": 0,
    "size": 10,
    "totalPages": 5,
    "totalElements": 50
  }
}
```

#### 搜索
```http
GET /api/articles/search?keyword=Spring&page=0&size=10
```

#### 点赞
```http
POST /api/articles/1/like
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

// 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "likeCount": 11,
    "liked": true
  }
}
```

**Authentication参数**:
- Spring Security自动注入
- 包含当前登录用户信息
- 未登录时为null
- 通过`authentication.getName()`获取用户名

---

### 3. 其他控制器概览

#### CategoryController
```java
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    // GET /api/categories - 获取所有分类
    // GET /api/categories/{id} - 获取分类详情
}
```

#### TagController
```java
@RestController
@RequestMapping("/api/tags")
public class TagController {
    // GET /api/tags - 获取所有标签
    // GET /api/tags/{id} - 获取标签详情
}
```

#### CommentController
```java
@RestController
@RequestMapping("/api/comments")
public class CommentController {
    // GET /api/comments/article/{id} - 获取文章评论
    // POST /api/comments/article/{id} - 发表评论
}
```

#### SettingController
```java
@RestController
@RequestMapping("/api/settings")
public class SettingController {
    // GET /api/settings - 获取系统设置
}
```

#### AdminArticleController (管理后台)
```java
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {
    // POST /api/admin/articles - 创建文章
    // PUT /api/admin/articles/{id} - 更新文章
    // DELETE /api/admin/articles/{id} - 删除文章
    // GET /api/admin/articles - 获取所有文章（含草稿）
}
```

## 🎯 设计模式

### 1. RESTful设计
```java
// 资源操作映射
GET    /api/articles        → 查询列表
GET    /api/articles/{id}   → 查询单个
POST   /api/articles        → 创建
PUT    /api/articles/{id}   → 更新
DELETE /api/articles/{id}   → 删除
```

### 2. 统一响应封装
```java
// 所有方法返回 Result<T>
public Result<PageResult<ArticleDTO>> getArticles(...) {
    return Result.success(PageResult.of(...));
}
```

### 3. 参数验证
```java
@PostMapping("/register")
public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // @Valid 自动验证DTO中的注解
}
```

### 4. 条件处理
```java
// 根据认证状态获取用户名
String username = authentication != null ? authentication.getName() : null;
```

### 5. 错误处理
```java
// 业务异常自动转换为HTTP响应
throw BusinessException.unauthorized("Unauthorized");
// → 401 {"code": 401, "message": "Unauthorized", "data": null}
```

## 📊 请求处理流程

### 完整流程示例
```
1. HTTP请求: GET /api/articles/1
   ↓
2. Spring MVC路由到: ArticleController.getArticleById()
   ↓
3. 参数解析: @PathVariable Long id, Authentication auth
   ↓
4. 调用Service: articleService.getArticleById(id, username)
   ↓
5. Service调用Repository: articleRepository.findById(id)
   ↓
6. Repository查询数据库
   ↓
7. Entity转换为DTO: ArticleDTO.fromEntity(article)
   ↓
8. Service返回DTO给Controller
   ↓
9. Controller封装Result: Result.success(articleDTO)
   ↓
10. Jackson序列化为JSON
    ↓
11. HTTP响应: {"code": 200, "message": "success", "data": {...}}
```

## 🔍 参数绑定

### 路径参数
```java
@GetMapping("/{id}")
public Result<ArticleDTO> getArticleById(@PathVariable Long id) { ... }
```

### 查询参数
```java
@GetMapping
public Result<PageResult<ArticleDTO>> getArticles(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) { ... }
```

### 请求体
```java
@PostMapping
public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { ... }
```

### 认证信息
```java
@GetMapping("/{id}")
public Result<ArticleDTO> getArticleById(
    @PathVariable Long id,
    Authentication authentication) { ... }
```

## 📝 统一响应格式

### 成功响应
```java
// 无数据
Result.success()
// → {"code": 200, "message": "success", "data": null}

// 有数据
Result.success(data)
// → {"code": 200, "message": "success", "data": {...}}

// 自定义消息
Result.success("创建成功", data)
// → {"code": 200, "message": "创建成功", "data": {...}}
```

### 错误响应
```java
Result.badRequest("参数错误")      // 400
Result.unauthorized("未登录")      // 401
Result.forbidden("无权限")         // 403
Result.notFound("资源不存在")      // 404
Result.error("服务器错误")         // 500
```

### 分页响应
```java
// PageResult包装类
PageResult.of(page)  // 自动转换Page<T>为分页对象
```

## 🎨 响应数据结构

### 文章DTO
```json
{
  "id": 1,
  "title": "文章标题",
  "slug": "article-slug",
  "summary": "摘要",
  "content": "内容",
  "coverImage": "/uploads/2024/01/xxx.jpg",
  "status": "PUBLISHED",
  "viewCount": 100,
  "likeCount": 10,
  "publishedAt": "2024-01-01T10:00:00",
  "author": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "avatar": "/avatar.jpg"
  },
  "category": {
    "id": 1,
    "name": "技术",
    "slug": "tech"
  },
  "tags": [
    {"id": 1, "name": "Java", "slug": "java"},
    {"id": 2, "name": "Spring", "slug": "spring"}
  ],
  "liked": true  // 当前用户是否点赞
}
```

### 评论DTO
```json
{
  "id": 1,
  "content": "评论内容",
  "authorName": "张三",
  "authorEmail": "zhang@example.com",
  "authorUrl": "https://example.com",
  "status": "APPROVED",
  "createdAt": "2024-01-01T10:00:00",
  "user": {
    "id": 1,
    "username": "user1",
    "nickname": "用户1"
  },
  "replies": [
    {
      "id": 2,
      "content": "回复内容",
      "parent": {"id": 1},
      ...
    }
  ]
}
```

## 🔐 安全配置

### 公开端点
```java
// SecurityConfig中配置
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
```

### 需要认证
```java
// 默认：所有其他端点需要认证
.anyRequest().authenticated()
```

### 需要管理员
```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

## 📊 错误处理

### 全局异常处理
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Result<Object>> handleBusinessException(BusinessException ex) {
    return ResponseEntity.status(ex.getCode())
        .body(Result.error(ex.getCode(), ex.getMessage()));
}
```

### 常见错误
| HTTP状态码 | 错误码 | 含义 |
|------------|--------|------|
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未登录或Token无效 |
| 403 | 403 | 无权限访问 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

## 🧪 测试示例

### MockMvc测试
```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("test");
        request.setPassword("password");
        request.setEmail("test@example.com");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("token"));
    }
}
```

## 📝 最佳实践

### 1. 单一职责
```java
// ✅ 推荐：Controller只负责请求响应
@GetMapping("/{id}")
public Result<ArticleDTO> getArticle(@PathVariable Long id) {
    return Result.success(articleService.getArticleById(id));
}

// ❌ 避免：在Controller写业务逻辑
@GetMapping("/{id}")
public Result<ArticleDTO> getArticle(@PathVariable Long id) {
    Article article = articleRepository.findById(id).orElseThrow(); // 业务逻辑
    // ...
}
```

### 2. 参数验证
```java
// ✅ 推荐：使用@Valid
public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request)

// ❌ 避免：手动验证
public Result<AuthResponse> register(@RequestBody RegisterRequest request) {
    if (request.getUsername() == null) { ... } // 手动验证
}
```

### 3. 统一响应
```java
// ✅ 推荐：使用Result封装
return Result.success(data);

// ❌ 避免：直接返回实体
return article; // 缺少统一格式
```

### 4. 认证处理
```java
// ✅ 推荐：使用Authentication参数
public Result<?> method(Authentication auth) {
    String username = auth != null ? auth.getName() : null;
}

// ❌ 避免：从Request手动获取
String token = request.getHeader("Authorization"); // 手动解析
```

### 5. 分页规范
```java
// ✅ 推荐：统一分页参数和排序
Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());

// ❌ 避免：分页参数不一致
PageRequest.of(page, size) // 缺少排序
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队