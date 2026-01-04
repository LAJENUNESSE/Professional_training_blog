# 快速参考指南

## 🎯 API速查表

### 认证相关
```
POST /api/auth/register    # 注册
POST /api/auth/login       # 登录
POST /api/auth/refresh     # 刷新Token
```

### 文章相关
```
GET  /api/articles                 # 文章列表（分页）
GET  /api/articles/{id}            # 文章详情（ID）
GET  /api/articles/slug/{slug}     # 文章详情（Slug）
GET  /api/articles/category/{id}   # 分类文章
GET  /api/articles/tag/{id}        # 标签文章
GET  /api/articles/search          # 搜索
POST /api/articles/{id}/like       # 点赞
```

### 评论相关
```
GET  /api/comments/article/{id}    # 获取评论
POST /api/comments/article/{id}    # 发表评论
```

### 管理后台（需要ADMIN）
```
GET    /api/admin/articles         # 文章列表
POST   /api/admin/articles         # 创建文章
PUT    /api/admin/articles/{id}    # 更新文章
DELETE /api/admin/articles/{id}    # 删除文章

GET    /api/admin/comments         # 评论管理
POST   /api/admin/comments/{id}/approve  # 通过评论
POST   /api/admin/comments/{id}/reject   # 拒绝评论
```

## 📝 响应格式

### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 分页响应
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalPages": 5,
    "totalElements": 50
  }
}
```

### 错误响应
```json
{
  "code": 400,
  "message": "错误描述",
  "data": null
}
```

## 🔑 常用类型

### ArticleDTO
```typescript
{
  id: number
  title: string
  slug: string
  summary: string | null
  content: string
  coverImage: string | null
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  isTop: boolean
  allowComment: boolean
  viewCount: number
  likeCount: number
  liked?: boolean
  publishedAt: string | null
  author: { id, username, nickname, avatar }
  category: { id, name, slug } | null
  tags: { id, name, slug }[]
  createdAt: string
  updatedAt: string
}
```

### AuthResponse
```typescript
{
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: {
    id: number
    username: string
    email: string
    nickname: string | null
    avatar: string | null
    role: 'ADMIN' | 'USER'
  }
}
```

## 🛠️ 常用命令

### 后端
```bash
# 编译
./mvnw compile

# 运行
./mvnw spring-boot:run

# 测试
./mvnw test

# 打包
./mvnw package -DskipTests
```

### 前端
```bash
# 安装依赖
npm install

# 开发模式
npm run dev:web    # 前台
npm run dev:admin  # 后台

# 构建
npm run build

# 代码检查
npm run lint

# 格式化
npm run format
```

### 脚本
```bash
# 一键启动所有服务
./scripts/start-all.sh

# 仅启动后端
./scripts/start-backend.sh

# 仅启动前端
./scripts/start-frontend.sh
```

## 🎨 前端组件

### 前台组件
- `ArticleCard.vue` - 文章卡片
- `ArticleList.vue` - 文章列表
- `CommentList.vue` - 评论列表
- `DefaultLayout.vue` - 默认布局

### 后台组件
- `AdminLayout.vue` - 管理布局
- `DataTable.vue` - 数据表格
- `ArticleEditor.vue` - 文章编辑器

## 📊 数据库表结构

### users
```
id, username, password, email, nickname, avatar, role, enabled, created_at, updated_at
```

### articles
```
id, title, slug, summary, content, cover_image, status, is_top, allow_comment,
view_count, like_count, published_at, author_id, category_id, created_at, updated_at
```

### comments
```
id, content, author_name, author_email, author_url, status, ip_address, user_agent,
article_id, user_id, parent_id, created_at, updated_at
```

### categories
```
id, name, slug, description, sort_order, created_at, updated_at
```

### tags
```
id, name, slug, created_at, updated_at
```

### article_likes
```
id, article_id, user_id, created_at, updated_at
```

### settings
```
id, key, value, description, created_at, updated_at
```

## 🔐 安全配置

### 公开端点
- `/api/auth/**`
- `GET /api/articles/**`
- `GET /api/categories/**`
- `GET /api/tags/**`
- `GET/POST /api/comments/**`
- `GET /api/settings/**`

### 需要登录
- `POST /api/articles/{id}/like`
- 其他未公开的API

### 需要管理员
- `/api/admin/**`

## 🎯 开发提示

### 1. 类型安全
```typescript
// 共享类型
import type { ArticleDTO } from '@blog/shared'

// API调用
const result = await articleApi.getById(1)
const article: ArticleDTO = result.data
```

### 2. 状态管理
```typescript
// 使用Pinia
const authStore = useAuthStore()
authStore.login(data)
console.log(authStore.isAuthenticated)
```

### 3. 分页处理
```typescript
// 使用usePagination
const { data, loading, fetch, loadMore } = usePagination(
  (page, size) => articleApi.getPublished({ page, size })
)
```

### 4. 错误处理
```typescript
try {
  await apiCall()
} catch (error) {
  if (error instanceof Error) {
    console.error(error.message)
  }
}
```

## 📋 检查清单

### 新功能开发
- [ ] 后端：Entity → Repository → Service → Controller
- [ ] 前端：Type → API → Component → Route
- [ ] 测试：单元测试 + 集成测试
- [ ] 文档：更新API文档

### 部署前
- [ ] 运行测试
- [ ] 代码格式化
- [ ] 构建前端
- [ ] 打包后端
- [ ] 检查配置

---

**文档版本**: v1.0
**最后更新**: 2026-01-04