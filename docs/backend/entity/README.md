# 后端实体层 (Entity Layer)

## 📋 概述

实体层是JPA持久化层的核心，定义了数据库表结构与Java对象的映射关系。所有实体继承自`BaseEntity`，包含统一的审计字段。

## 🏗️ 实体关系图

```
User (用户)
  ↓ 1:N
Article (文章) ────┐
  ↓ 1:N            │
Comment (评论)     │
  ↑ N:1            │
  └────────────────┘
  ↑ N:1            │
ArticleLike (点赞) │
  ↑ N:1            │
  └────────────────┘
  ↑ N:1            │
Category (分类) ───┘
  ↑ N:1            │
Tag (标签) ────────┘
  ↑ N:N            │
  └────────────────┘
Setting (设置)
```

## 📚 实体详解

### 1. BaseEntity (基础实体)

**文件**: `src/main/java/com/example/blog/entity/BaseEntity.java`

**功能描述**: 所有实体的基类，提供统一的ID和审计字段。

**核心代码**:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| id | Long | 主键 | 自增 |
| createdAt | LocalDateTime | 创建时间 | 自动填充，不可更新 |
| updatedAt | LocalDateTime | 更新时间 | 自动更新 |

**设计模式**:
- **模板方法模式**: 定义通用字段结构
- **观察者模式**: 通过`@EntityListeners`监听实体事件

---

### 2. User (用户实体)

**文件**: `src/main/java/com/example/blog/entity/User.java`

**功能描述**: 系统用户，包含认证信息和个人资料。

**核心代码**:
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;  // BCrypt加密

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 50)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    public enum Role {
        ADMIN, USER
    }
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| username | String | 用户名 | 唯一，50字符 |
| password | String | 密码(加密) | BCrypt哈希 |
| email | String | 邮箱 | 唯一，100字符 |
| nickname | String | 昵称 | 可选，50字符 |
| avatar | String | 头像URL | 可选，255字符 |
| role | Role | 角色 | ADMIN/USER |
| enabled | Boolean | 是否启用 | 默认true |

**关联关系**:
- **1:N** → Article (文章作者)
- **1:N** → Comment (评论者)
- **1:N** → ArticleLike (点赞者)

**业务规则**:
- 用户名和邮箱必须唯一
- 密码在存储前必须BCrypt加密
- 默认角色为USER
- 管理员通过role字段标识

---

### 3. Article (文章实体)

**文件**: `src/main/java/com/example/blog/entity/Article.java`

**功能描述**: 博客文章，包含内容、元数据和关联信息。

**核心代码**:
```java
@Entity
@Table(name = "articles")
public class Article extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 255)
    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(nullable = false)
    private Boolean isTop = false;

    @Column(nullable = false)
    private Boolean allowComment = true;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Integer likeCount = 0;

    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "article_tags",
        joinColumns = @JoinColumn(name = "article_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "article", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    public enum Status {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| title | String | 标题 | 必填，200字符 |
| slug | String | URL友好标识 | 可选，255字符 |
| summary | String | 摘要 | 可选，TEXT |
| content | String | 内容 | 必填，TEXT |
| coverImage | String | 封面图URL | 可选，255字符 |
| status | Status | 状态 | DRAFT/PUBLISHED/ARCHIVED |
| isTop | Boolean | 置顶 | 默认false |
| allowComment | Boolean | 允许评论 | 默认true |
| viewCount | Integer | 浏览数 | 默认0 |
| likeCount | Integer | 点赞数 | 默认0 |
| publishedAt | LocalDateTime | 发布时间 | 状态为PUBLISHED时填充 |

**关联关系**:
| 关系 | 类型 | 目标 | 描述 |
|------|------|------|------|
| author | ManyToOne | User | 文章作者 (必填) |
| category | ManyToOne | Category | 所属分类 (可选) |
| tags | ManyToMany | Tag | 标签集合 |
| comments | OneToMany | Comment | 评论列表 |

**业务逻辑**:
- **状态管理**: DRAFT(草稿) → PUBLISHED(发布) → ARCHIVED(归档)
- **置顶功能**: isTop控制排序优先级
- **统计计数**: viewCount和likeCount独立维护
- **发布时间**: 仅在状态变为PUBLISHED时自动填充
- **级联删除**: 删除文章时级联删除评论

---

### 4. Comment (评论实体)

**文件**: `src/main/java/com/example/blog/entity/Comment.java`

**功能描述**: 文章评论，支持层级回复。

**核心代码**:
```java
@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String authorName;

    @Column(length = 100)
    private String authorEmail;

    @Column(length = 255)
    private String authorUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Comment> replies = new ArrayList<>();

    public enum Status {
        PENDING, APPROVED, REJECTED
    }
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| content | String | 评论内容 | 必填，TEXT |
| authorName | String | 作者名 | 可选，50字符 |
| authorEmail | String | 作者邮箱 | 可选，100字符 |
| authorUrl | String | 作者网站 | 可选，255字符 |
| status | Status | 审核状态 | PENDING/APPROVED/REJECTED |
| ipAddress | String | IP地址 | 可选，50字符 |
| userAgent | String | 浏览器信息 | 可选，255字符 |

**关联关系**:
| 关系 | 类型 | 目标 | 描述 |
|------|------|------|------|
| article | ManyToOne | Article | 所属文章 (必填) |
| user | ManyToOne | User | 登录用户 (可选) |
| parent | ManyToOne | Comment | 父评论 (可选) |
| replies | OneToMany | Comment | 子评论列表 |

**层级结构**:
```
文章
├── 评论1 (parent=null)
│   ├── 回复1 (parent=评论1)
│   └── 回复2 (parent=评论1)
└── 评论2 (parent=null)
```

**业务逻辑**:
- **审核机制**: 默认PENDING，需管理员APPROVED才显示
- **匿名评论**: 可不登录，需填写authorName
- **登录评论**: 自动填充user信息
- **层级回复**: 通过parent字段构建树形结构
- **追踪信息**: 记录IP和UserAgent用于反垃圾

---

### 5. ArticleLike (文章点赞)

**文件**: `src/main/java/com/example/blog/entity/ArticleLike.java`

**功能描述**: 用户对文章的点赞记录，防止重复点赞。

**核心代码**:
```java
@Entity
@Table(name = "article_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"article_id", "user_id"})
})
public class ArticleLike extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| article | ManyToOne | Article | 点赞文章 |
| user | ManyToOne | User | 点赞用户 |

**约束**:
- **唯一约束**: (article_id, user_id)组合唯一，防止重复点赞
- **必填字段**: 两个外键都不能为空

**业务逻辑**:
- **点赞**: 插入记录，文章likeCount+1
- **取消点赞**: 删除记录，文章likeCount-1
- **状态查询**: 检查是否存在记录判断用户是否点赞

---

### 6. Category (分类实体)

**文件**: `src/main/java/com/example/blog/entity/Category.java`

**功能描述**: 文章分类，用于组织和导航。

**核心代码**:
```java
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 100)
    private String slug;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Article> articles = new ArrayList<>();
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| name | String | 分类名 | 唯一，必填，50字符 |
| slug | String | URL标识 | 可选，100字符 |
| description | String | 描述 | 可选，255字符 |
| sortOrder | Integer | 排序值 | 默认0，越小越靠前 |

**关联关系**:
- **1:N** → Article (分类下的文章)

**业务规则**:
- 分类名必须唯一
- 排序值控制前端显示顺序
- 删除分类时，文章category字段置为null

---

### 7. Tag (标签实体)

**文件**: `src/main/java/com/example/blog/entity/Tag.java`

**功能描述**: 文章标签，支持多标签关联。

**核心代码**:
```java
@Entity
@Table(name = "tags")
public class Tag extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 100)
    private String slug;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Article> articles = new HashSet<>();
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| name | String | 标签名 | 唯一，必填，50字符 |
| slug | String | URL标识 | 可选，100字符 |

**关联关系**:
- **N:M** ←→ Article (多对多)

**业务规则**:
- 标签名必须唯一
- 通过Article.tags间接关联文章

---

### 8. Setting (系统设置)

**文件**: `src/main/java/com/example/blog/entity/Setting.java`

**功能描述**: 系统配置项，键值对存储。

**核心代码**:
```java
@Entity
@Table(name = "settings")
public class Setting extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(length = 255)
    private String description;
}
```

**字段说明**:
| 字段 | 类型 | 描述 | 约束 |
|------|------|------|------|
| key | String | 配置键 | 唯一，必填，50字符 |
| value | String | 配置值 | 可选，TEXT |
| description | String | 描述 | 可选，255字符 |

**使用场景**:
- 网站标题、描述
- SEO配置
- 功能开关
- 自定义文案

## 🎯 设计模式总结

### 1. 继承模式
所有实体继承`BaseEntity`，统一审计字段。

### 2. 枚举模式
使用Java枚举表示有限状态：
- `User.Role`: ADMIN, USER
- `Article.Status`: DRAFT, PUBLISHED, ARCHIVED
- `Comment.Status`: PENDING, APPROVED, REJECTED

### 3. 关联模式
- **ManyToOne**: 单向关联（文章→用户）
- **OneToMany**: 双向关联（用户→文章）
- **ManyToMany**: 中间表（文章↔标签）
- **Self-Referencing**: 评论层级（评论→父评论）

### 4. 懒加载模式
默认使用`FetchType.LAZY`优化性能，避免N+1查询。

### 5. 级联模式
- 文章删除 → 级联删除评论
- 分类删除 → 文章category置null

## 📊 数据库映射

### 表结构概览
```sql
-- 用户表
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 文章表
CREATE TABLE articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(255),
    summary TEXT,
    content TEXT NOT NULL,
    cover_image VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_top BOOLEAN NOT NULL DEFAULT 0,
    allow_comment BOOLEAN NOT NULL DEFAULT 1,
    view_count INTEGER NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0,
    published_at DATETIME,
    author_id INTEGER NOT NULL,
    category_id INTEGER,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (author_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 中间表（文章标签）
CREATE TABLE article_tags (
    article_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);
```

## 🔍 最佳实践

### 1. 使用Lombok
```java
@Entity
@Getter
@Setter
@Builder  // 可选：构建器模式
@ToString(exclude = {"author", "category"})  // 避免循环引用
public class Article extends BaseEntity { ... }
```

### 2. 字段验证
```java
@Column(nullable = false, length = 200)
@NotBlank(message = "标题不能为空")
@Size(max = 200, message = "标题不能超过200字符")
private String title;
```

### 3. 枚举处理
```java
@Enumerated(EnumType.STRING)  // 存储字符串而非索引
private Status status;
```

### 4. 懒加载优化
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id", nullable = false)
private User author;
```

### 5. 级联策略
```java
@OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Comment> comments = new ArrayList<>();
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队