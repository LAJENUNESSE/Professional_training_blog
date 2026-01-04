# 前端共享库 (@blog/shared)

## 📋 概述

`@blog/shared` 是一个Monorepo共享库，包含所有前端应用（Web前台和Admin后台）共用的代码：
- **API客户端**: Axios封装，自动Token管理
- **TypeScript类型**: 与后端完全一致的类型定义
- **工具函数**: 存储、分页等通用工具
- **组合式函数**: Pinia Store和Vue Composables

## 🏗️ 包结构

```
frontend/packages/shared/
├── src/
│   ├── api/              # API客户端
│   │   ├── request.ts    # Axios实例 + Token刷新
│   │   ├── auth.ts       # 认证API
│   │   ├── article.ts    # 文章API
│   │   ├── category.ts   # 分类API
│   │   ├── tag.ts        # 标签API
│   │   ├── comment.ts    # 评论API
│   │   ├── setting.ts    # 设置API
│   │   ├── user.ts       # 用户API
│   │   └── upload.ts     # 上传API
│   ├── types/            # TypeScript类型定义
│   │   ├── api.ts        # 通用响应类型
│   │   ├── auth.ts       # 认证相关类型
│   │   ├── article.ts    # 文章相关类型
│   │   ├── category.ts   # 分类相关类型
│   │   ├── tag.ts        # 标签相关类型
│   │   ├── comment.ts    # 评论相关类型
│   │   ├── setting.ts    # 设置相关类型
│   │   └── upload.ts     # 上传相关类型
│   ├── composables/      # 组合式函数
│   │   ├── useAuth.ts    # 认证Store
│   │   └── usePagination.ts  # 分页Hook
│   ├── utils/            # 工具函数
│   │   └── storage.ts    # 本地存储封装
│   └── constants/        # 常量定义
│       └── index.ts
├── package.json
└── tsconfig.json
```

## 📚 核心模块详解

### 1. API客户端 (request.ts)

**功能描述**: Axios实例封装，提供自动Token注入、Token刷新、错误处理。

**核心代码**:
```typescript
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { tokenStorage } from '../utils/storage'
import { API_BASE_URL } from '../constants'

// 扩展Axios类型以支持响应拦截器返回data
export interface ApiInstance extends AxiosInstance {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const request: ApiInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Token刷新状态管理
let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

// 请求拦截器：自动添加Token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：Token刷新 + 错误处理
request.interceptors.response.use(
  (response) => response.data,  // 直接返回data，简化调用
  async (error) => {
    const originalRequest = error.config

    // 401错误，尝试刷新Token
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 等待刷新完成
        return new Promise((resolve) => {
          subscribeTokenRefresh((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshToken = tokenStorage.getRefreshToken()
        if (!refreshToken) throw new Error('No refresh token')

        // 调用刷新API
        const response = await axios.post(
          `${API_BASE_URL}/api/auth/refresh`,
          null,
          { params: { refreshToken } }
        )

        const { accessToken, refreshToken: newRefresh } = response.data.data
        tokenStorage.setAccessToken(accessToken)
        tokenStorage.setRefreshToken(newRefresh)

        onTokenRefreshed(accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`

        return request(originalRequest)
      } catch (refreshError) {
        tokenStorage.clear()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 其他错误
    const message = error.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

// Token刷新队列管理
function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

export default request
```

**设计模式**:

#### 1. Token自动刷新
```
请求 → 401错误 → 检查刷新状态
  ├─ 正在刷新 → 加入等待队列
  └─ 未刷新 → 锁定 → 调用刷新API → 更新Token → 重发原请求
```

#### 2. 响应拦截器优化
```typescript
// 原始：response.data.data.xxx
// 优化：response.xxx
(response) => response.data
```

#### 3. 并发请求处理
```typescript
// 多个401请求同时到达
// 1. 第一个请求开始刷新
// 2. 后续请求加入队列
// 3. 刷新完成后批量重发
```

**使用示例**:
```typescript
import { articleApi } from '@blog/shared'

// 获取文章列表（自动处理Token）
const result = await articleApi.getPublished({ page: 0, size: 10 })

// 创建文章（需要登录，自动添加Token）
const article = await articleApi.admin.create({
  title: '新文章',
  content: '内容',
  status: 'PUBLISHED'
})
```

---

### 2. 类型定义 (types/)

**功能描述**: 提供与后端完全一致的TypeScript类型，确保前后端类型安全。

#### API响应类型
```typescript
// api.ts
export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  content: T[]
  page: number
  size: number
  totalPages: number
  totalElements: number
}

export interface PageParams {
  page?: number
  size?: number
}
```

#### 认证类型
```typescript
// auth.ts
export type Role = 'ADMIN' | 'USER'

export interface UserInfo {
  id: number
  username: string
  email: string
  nickname: string | null
  avatar: string | null
  role: Role
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}
```

#### 文章类型
```typescript
// article.ts
export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface ArticleDTO {
  id: number
  title: string
  slug: string
  summary: string | null
  content: string
  coverImage: string | null
  status: ArticleStatus
  isTop: boolean
  allowComment: boolean
  viewCount: number
  likeCount: number
  liked?: boolean | null
  publishedAt: string | null
  author: AuthorInfo
  category: CategoryDTO | null
  tags: TagDTO[]
  commentCount: number
  createdAt: string
  updatedAt: string
}

export interface ArticleRequest {
  title: string
  slug?: string
  summary?: string
  content: string
  coverImage?: string
  status?: ArticleStatus
  isTop?: boolean
  allowComment?: boolean
  categoryId?: number
  tagIds?: number[]
}
```

**类型映射**:
| 后端Java | 前端TypeScript | 说明 |
|----------|----------------|------|
| `Result<T>` | `Result<T>` | 统一响应 |
| `PageResult<T>` | `PageResult<T>` | 分页响应 |
| `ArticleDTO` | `ArticleDTO` | 文章响应 |
| `ArticleRequest` | `ArticleRequest` | 文章请求 |
| `Article.Status` | `ArticleStatus` | 文章状态枚举 |

---

### 3. API模块 (api/)

**功能描述**: 按资源组织的API调用封装。

#### 认证API
```typescript
export const authApi = {
  login: (data: LoginRequest) =>
    request.post<Result<AuthResponse>>('/api/auth/login', data),

  register: (data: RegisterRequest) =>
    request.post<Result<AuthResponse>>('/api/auth/register', data),

  refresh: (refreshToken: string) =>
    request.post<Result<AuthResponse>>('/api/auth/refresh', null, {
      params: { refreshToken },
    }),
}
```

#### 文章API
```typescript
export const articleApi = {
  // 公开接口
  getPublished: (params?: PageParams) =>
    request.get<Result<PageResult<ArticleDTO>>>('/api/articles', { params }),

  getById: (id: number) =>
    request.get<Result<ArticleDTO>>(`/api/articles/${id}`),

  getBySlug: (slug: string) =>
    request.get<Result<ArticleDTO>>(`/api/articles/slug/${slug}`),

  search: (keyword: string, params?: PageParams) =>
    request.get<Result<PageResult<ArticleDTO>>>('/api/articles/search', {
      params: { keyword, ...params },
    }),

  like: (id: number) =>
    request.post<Result<ArticleLikeResponse>>(`/api/articles/${id}/like`),

  // 管理接口
  admin: {
    getAll: (params?: PageParams & { status?: string; categoryId?: number; tagId?: number }) =>
      request.get<Result<PageResult<ArticleDTO>>>('/api/admin/articles', { params }),

    create: (data: ArticleRequest) =>
      request.post<Result<ArticleDTO>>('/api/admin/articles', data),

    update: (id: number, data: ArticleRequest) =>
      request.put<Result<ArticleDTO>>(`/api/admin/articles/${id}`, data),

    delete: (id: number) =>
      request.delete<Result<void>>(`/api/admin/articles/${id}`),
  },
}
```

**API组织原则**:
- **资源分组**: auth, article, category, tag, comment, setting, user, upload
- **权限分离**: 公开接口 vs admin接口
- **类型安全**: 每个方法都有明确的返回类型

---

### 4. 组合式函数 (composables/)

#### useAuthStore (认证Store)
```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api'
import { tokenStorage } from '../utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(tokenStorage.getUser<UserInfo>())
  const accessToken = ref<string | null>(tokenStorage.getAccessToken())

  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    const { accessToken: token, refreshToken, user: userInfo } = res.data
    accessToken.value = token
    user.value = userInfo
    tokenStorage.setAccessToken(token)
    tokenStorage.setRefreshToken(refreshToken)
    tokenStorage.setUser(userInfo)
    return res.data
  }

  async function register(data: RegisterRequest) {
    const res = await authApi.register(data)
    const { accessToken: token, refreshToken, user: userInfo } = res.data
    accessToken.value = token
    user.value = userInfo
    tokenStorage.setAccessToken(token)
    tokenStorage.setRefreshToken(refreshToken)
    tokenStorage.setUser(userInfo)
    return res.data
  }

  function logout() {
    accessToken.value = null
    user.value = null
    tokenStorage.clear()
  }

  function initAuth() {
    const token = tokenStorage.getAccessToken()
    const savedUser = tokenStorage.getUser<UserInfo>()
    if (token && savedUser) {
      accessToken.value = token
      user.value = savedUser
    }
  }

  return {
    user,
    accessToken,
    isAuthenticated,
    isAdmin,
    login,
    register,
    logout,
    initAuth,
  }
})
```

**功能**:
- ✅ 用户状态管理
- ✅ 登录/注册/登出
- ✅ 权限检查 (isAdmin)
- ✅ 持久化存储
- ✅ 初始化恢复

#### usePagination (分页Hook)
```typescript
import { ref, computed } from 'vue'
import type { PageResult } from '../types'
import { DEFAULT_PAGE_SIZE } from '../constants'

export function usePagination<T>(
  fetchFn: (page: number, size: number) => Promise<PageResult<T>>
) {
  const data = ref<T[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  const total = ref(0)
  const totalPages = ref(0)

  const hasMore = computed(() => currentPage.value < totalPages.value)
  const isEmpty = computed(() => !loading.value && data.value.length === 0)

  async function fetch(page = 1) {
    loading.value = true
    try {
      const result = await fetchFn(page - 1, pageSize.value)
      data.value = result.content
      currentPage.value = result.pageNumber + 1
      total.value = result.totalElements
      totalPages.value = result.totalPages
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (!hasMore.value || loading.value) return
    loading.value = true
    try {
      const result = await fetchFn(currentPage.value, pageSize.value)
      data.value = [...data.value, ...result.content]
      currentPage.value = result.pageNumber + 1
      total.value = result.totalElements
      totalPages.value = result.totalPages
    } finally {
      loading.value = false
    }
  }

  function reset() {
    data.value = []
    currentPage.value = 1
    total.value = 0
    totalPages.value = 0
  }

  return {
    data,
    loading,
    currentPage,
    pageSize,
    total,
    totalPages,
    hasMore,
    isEmpty,
    fetch,
    loadMore,
    reset,
  }
}
```

**使用示例**:
```typescript
// 在组件中使用
const { data, loading, fetch, loadMore, hasMore } = usePagination(
  (page, size) => articleApi.getPublished({ page, size })
)

// 初始加载
onMounted(() => fetch(1))

// 加载更多
const handleLoadMore = () => loadMore()
```

---

### 5. 工具函数 (utils/)

#### 本地存储封装
```typescript
const TOKEN_KEY = 'blog_access_token'
const REFRESH_KEY = 'blog_refresh_token'
const USER_KEY = 'blog_user'

export const tokenStorage = {
  getAccessToken: (): string | null => localStorage.getItem(TOKEN_KEY),

  setAccessToken: (token: string): void => localStorage.setItem(TOKEN_KEY, token),

  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_KEY),

  setRefreshToken: (token: string): void => localStorage.setItem(REFRESH_KEY, token),

  getUser: <T>(): T | null => {
    const user = localStorage.getItem(USER_KEY)
    return user ? JSON.parse(user) : null
  },

  setUser: <T>(user: T): void => localStorage.setItem(USER_KEY, JSON.stringify(user)),

  clear: (): void => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  },
}
```

**特点**:
- 类型安全（泛型）
- 统一Key管理
- 一键清除所有数据

---

### 6. 常量定义 (constants/)

```typescript
// 单体部署时使用相对路径，开发时使用环境变量
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export const ARTICLE_STATUS = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档',
} as const

export const COMMENT_STATUS = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
} as const

export const USER_ROLE = {
  ADMIN: '管理员',
  USER: '用户',
} as const

export const DEFAULT_PAGE_SIZE = 10
```

---

## 🎯 设计模式

### 1. 单例模式
```typescript
// Axios实例单例
const request: ApiInstance = axios.create({...})
export default request
```

### 2. 观察者模式
```typescript
// Token刷新队列
let refreshSubscribers: ((token: string) => void)[] = []

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
}
```

### 3. 工厂模式
```typescript
// Pinia Store工厂
export const useAuthStore = defineStore('auth', () => { ... })
```

### 4. 策略模式
```typescript
// 分页策略
export function usePagination<T>(fetchFn: ...) { ... }
// 可以传入任意API函数
```

### 5. 装饰器模式
```typescript
// 响应拦截器装饰
(response) => response.data  // 装饰原始响应
```

## 📊 数据流

### 认证流程
```
用户登录 → useAuthStore.login() → authApi.login()
    ↓
Token存储 → tokenStorage.setAccessToken() → localStorage
    ↓
API请求 → request拦截器 → 添加Authorization头
    ↓
响应处理 → 401 → Token刷新 → 重试请求
```

### 分页流程
```
usePagination(fetchFn) → fetch(page)
    ↓
调用API → request.get() → 后端分页
    ↓
返回数据 → 更新ref → 组件渲染
    ↓
loadMore() → 追加数据 → 无限滚动
```

## 🔧 环境配置

### .env
```env
# 开发环境
VITE_API_BASE_URL=http://localhost:8080

# 生产环境（单体部署）
VITE_API_BASE_URL=
```

### 使用环境变量
```typescript
// constants/index.ts
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
```

**单体部署优势**:
- 开发：`http://localhost:8080`
- 生产：空字符串（相对路径，同源）

## 🧪 类型安全

### 完整类型链
```typescript
// 1. 请求类型
const params: PageParams = { page: 0, size: 10 }

// 2. API返回类型
const result: Result<PageResult<ArticleDTO>> = await articleApi.getPublished(params)

// 3. 数据使用
const articles: ArticleDTO[] = result.data.content
articles.forEach(article => {
  console.log(article.title)  // 类型推断
})
```

### 错误处理类型
```typescript
try {
  await authApi.login(data)
} catch (error) {
  // error: unknown，需要类型守卫
  if (error instanceof Error) {
    console.error(error.message)
  }
}
```

## 📝 最佳实践

### 1. API调用
```typescript
// ✅ 推荐：使用封装的API
import { articleApi } from '@blog/shared'
const result = await articleApi.getPublished({ page: 0, size: 10 })

// ❌ 避免：直接使用axios
import axios from 'axios'
const result = await axios.get('/api/articles', { params: { page: 0 } })
```

### 2. 类型定义
```typescript
// ✅ 推荐：使用共享类型
import type { ArticleDTO } from '@blog/shared'
const article: ArticleDTO = ...

// ❌ 避免：重复定义类型
interface MyArticle { ... }  // 与ArticleDTO重复
```

### 3. 状态管理
```typescript
// ✅ 推荐：使用Pinia Store
const authStore = useAuthStore()
authStore.login(data)

// ❌ 避免：手动管理状态
const user = ref(null)  // 无法跨组件共享
```

### 4. 分页处理
```typescript
// ✅ 推荐：使用usePagination
const { data, loading, fetch } = usePagination(articleApi.getPublished)

// ❌ 避免：手动实现分页
const page = ref(0)
const data = ref([])
async function load() {
  const res = await articleApi.getPublished({ page: page.value })
  data.value = res.data.content
  // 需要手动管理状态
}
```

## 📦 导出结构

```typescript
// @blog/shared
├── API
│   ├── request          // Axios实例
│   ├── authApi          // 认证API
│   ├── articleApi       // 文章API
│   ├── categoryApi      // 分类API
│   ├── tagApi           // 标签API
│   ├── commentApi       // 评论API
│   ├── settingApi       // 设置API
│   ├── userApi          // 用户API
│   └── uploadApi        // 上传API
├── Types
│   ├── Result           // 响应封装
│   ├── PageResult       // 分页响应
│   ├── ArticleDTO       // 文章类型
│   ├── AuthResponse     // 认证响应
│   └── ...              // 其他类型
├── Composables
│   ├── useAuthStore     // 认证Store
│   └── usePagination    // 分页Hook
├── Utils
│   └── tokenStorage     // 存储封装
└── Constants
    ├── API_BASE_URL
    ├── ARTICLE_STATUS
    └── DEFAULT_PAGE_SIZE
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队