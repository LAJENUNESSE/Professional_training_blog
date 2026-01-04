# 前台博客应用 (@blog/web)

## 📋 概述

`@blog/web` 是博客系统的前台应用，面向普通访客，提供文章浏览、搜索、评论、点赞等功能。

**访问地址**: http://localhost:3000

## 🏗️ 项目结构

```
frontend/packages/web/
├── src/
│   ├── main.ts              # 应用入口
│   ├── App.vue              # 根组件
│   ├── router/              # 路由配置
│   │   └── index.ts
│   ├── stores/              # Pinia状态管理
│   │   ├── index.ts
│   │   └── theme.ts         # 主题Store
│   ├── views/               # 页面组件
│   │   ├── HomeView.vue     # 首页
│   │   ├── ArticleView.vue  # 文章详情
│   │   ├── CategoryView.vue # 分类页面
│   │   ├── TagView.vue      # 标签页面
│   │   ├── SearchView.vue   # 搜索页面
│   │   ├── LoginView.vue    # 登录
│   │   ├── RegisterView.vue # 注册
│   │   └── NotFoundView.vue # 404
│   ├── components/          # 通用组件
│   │   ├── article/
│   │   │   ├── ArticleCard.vue     # 文章卡片
│   │   │   └── ArticleList.vue     # 文章列表
│   │   ├── CommentList.vue         # 评论列表
│   │   └── Pagination.vue          # 分页组件
│   ├── layouts/             # 布局组件
│   │   └── DefaultLayout.vue       # 默认布局
│   ├── styles/              # 样式文件
│   │   └── main.css
│   └── assets/              # 静态资源
├── vite.config.ts           # Vite配置
├── package.json
└── tsconfig.json
```

## 📚 核心页面详解

### 1. 首页 (HomeView.vue)

**功能**: 展示文章列表，支持分页加载

**核心代码**:
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { articleApi, type ArticleDTO, type PageResult } from '@blog/shared'
import ArticleCard from '@/components/article/ArticleCard.vue'

const articles = ref<ArticleDTO[]>([])
const loading = ref(true)
const currentPage = ref(0)
const totalPages = ref(0)
const hasMore = ref(false)

async function loadArticles(page = 0) {
  loading.value = true
  try {
    const res = (await articleApi.getPublished({ page, size: 10 })) as unknown as {
      data: PageResult<ArticleDTO>
    }
    if (page === 0) {
      articles.value = res.data.content
    } else {
      articles.value = [...articles.value, ...res.data.content]
    }
    currentPage.value = res.data.pageNumber
    totalPages.value = res.data.totalPages
    hasMore.value = !res.data.last
  } catch (error) {
    console.error('Failed to load articles:', error)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  if (hasMore.value && !loading.value) {
    loadArticles(currentPage.value + 1)
  }
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div>
    <!-- 加载状态 -->
    <div v-if="loading && articles.length === 0" class="flex justify-center py-12">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && articles.length === 0" class="text-center py-12 text-gray-500">
      暂无文章
    </div>

    <!-- 文章列表 -->
    <div v-else>
      <ArticleCard v-for="article in articles" :key="article.id" :article="article" />

      <!-- 加载更多 -->
      <div v-if="hasMore" class="py-8 text-center">
        <button
          @click="loadMore"
          :disabled="loading"
          class="px-6 py-2 bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>

      <!-- 没有更多 -->
      <div v-else class="py-8 text-center text-gray-400 dark:text-gray-500 text-sm">
        没有更多了
      </div>
    </div>
  </div>
</template>
```

**特性**:
- ✅ 无限滚动/加载更多
- ✅ 加载状态反馈
- ✅ 空状态处理
- ✅ 响应式设计

---

### 2. 文章详情 (ArticleView.vue)

**功能**: 文章展示、Markdown渲染、代码高亮、点赞、评论

**核心代码**:
```vue
<script setup lang="ts">
import { ref, onMounted, computed, watch, reactive } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import {
  articleApi,
  commentApi,
  type ArticleDTO,
  type CommentDTO,
  type CommentRequest,
  useAuthStore,
  formatDate,
} from '@blog/shared'
import 'highlight.js/styles/github-dark.css'

const route = useRoute()
const authStore = useAuthStore()
const article = ref<ArticleDTO | null>(null)
const comments = ref<CommentDTO[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const liked = ref<boolean>(false)
const likeLoading = ref(false)

// 评论表单
const commentForm = reactive<CommentRequest>({
  content: '',
  authorName: '',
  authorEmail: '',
  authorUrl: '',
})
const submittingComment = ref(false)
const commentFeedback = ref<string | null>(null)
const commentError = ref<string | null>(null)

// Markdown渲染器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (_) {}
    }
    return ''
  },
})

const renderedContent = computed(() => {
  if (!article.value) return ''
  return md.render(article.value.content)
})

// 加载文章
async function loadArticle() {
  loading.value = true
  error.value = null
  commentFeedback.value = null
  commentError.value = null
  commentForm.content = ''

  try {
    const slug = route.params.slug as string
    const res = (await articleApi.getBySlug(slug)) as unknown as { data: ArticleDTO }
    article.value = res.data
    liked.value = !!res.data.liked

    // 加载评论
    const commentsRes = (await commentApi.getByArticle(res.data.id)) as unknown as {
      data: CommentDTO[]
    }
    comments.value = commentsRes.data.filter((c: CommentDTO) => c.status === 'APPROVED')
  } catch (err) {
    error.value = '文章加载失败'
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 点赞处理
async function handleLike() {
  if (!article.value || likeLoading.value) return
  if (!authStore.isAuthenticated) {
    window.alert('请先登录后点赞')
    return
  }
  likeLoading.value = true
  try {
    const res = (await articleApi.like(article.value.id)) as unknown as {
      data: { likeCount: number; liked: boolean }
    }
    article.value.likeCount = res.data.likeCount
    liked.value = res.data.liked
  } catch (err) {
    console.error('Failed to like:', err)
    window.alert('操作失败，请稍后重试')
  } finally {
    likeLoading.value = false
  }
}

// 提交评论
async function handleSubmitComment() {
  if (!article.value || submittingComment.value) return
  if (!commentForm.content?.trim()) {
    commentError.value = '请输入评论内容'
    return
  }
  if (!authStore.isAuthenticated && !commentForm.authorName?.trim()) {
    commentError.value = '请填写昵称，便于展示'
    return
  }

  commentError.value = null
  commentFeedback.value = null
  submittingComment.value = true

  try {
    const payload: CommentRequest = {
      content: commentForm.content.trim(),
    }
    if (!authStore.isAuthenticated) {
      payload.authorName = commentForm.authorName?.trim() || '匿名用户'
      payload.authorEmail = commentForm.authorEmail?.trim() || undefined
      payload.authorUrl = commentForm.authorUrl?.trim() || undefined
    }

    const res = (await commentApi.create(article.value.id, payload)) as unknown as {
      data: CommentDTO
    }

    if (res.data.status === 'APPROVED') {
      comments.value.unshift(res.data)
      commentFeedback.value = '评论已发表'
    } else {
      commentFeedback.value = '评论已提交，待审核'
    }
    commentForm.content = ''
  } catch (err) {
    console.error('Failed to comment:', err)
    commentError.value = '评论提交失败，请稍后重试'
  } finally {
    submittingComment.value = false
  }
}

// 监听路由变化
watch(() => route.params.slug, loadArticle)

onMounted(loadArticle)
</script>
```

**特性**:
- ✅ Markdown渲染
- ✅ 代码语法高亮
- ✅ 点赞状态管理
- ✅ 评论提交（登录/匿名）
- ✅ 路由参数监听
- ✅ 错误处理

---

### 3. 搜索页面 (SearchView.vue)

**功能**: 关键词搜索文章

**核心逻辑**:
```typescript
const keyword = ref('')
const articles = ref<ArticleDTO[]>([])
const loading = ref(false)

async function handleSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const res = await articleApi.search(keyword.value.trim(), { page: 0, size: 20 })
    articles.value = res.data.content
  } finally {
    loading.value = false
  }
}
```

---

## 🎨 组件系统

### ArticleCard (文章卡片)
**位置**: `components/article/ArticleCard.vue`

**功能**: 单篇文章展示，用于列表

**Props**:
```typescript
interface Props {
  article: ArticleDTO
}
```

**特性**:
- 置顶标识
- 封面图（可选）
- 标题、摘要
- 作者、分类、标签
- 浏览量、发布时间
- 悬停效果

### DefaultLayout (默认布局)
**位置**: `layouts/DefaultLayout.vue`

**结构**:
```vue
<template>
  <div class="min-h-screen bg-white dark:bg-gray-900">
    <!-- 导航栏 -->
    <header>...</header>

    <!-- 主内容区 -->
    <main class="max-w-4xl mx-auto px-4 py-8">
      <slot />
    </main>

    <!-- 页脚 -->
    <footer>...</footer>
  </div>
</template>
```

---

## 🛠️ 技术栈

### 核心依赖
```json
{
  "vue": "^3.5.13",
  "vue-router": "^4.5.0",
  "pinia": "^2.3.0",
  "@blog/shared": "workspace:^",
  "markdown-it": "^14.1.0",
  "highlight.js": "^11.11.1"
}
```

### 开发工具
```json
{
  "vite": "^6.0.5",
  "vue-tsc": "^2.2.0",
  "tailwindcss": "^3.4.17",
  "unplugin-vue-components": "^0.27.5",
  "unplugin-icons": "^0.21.0"
}
```

---

## 📊 数据流

### 文章列表加载
```
HomeView.onMounted()
  ↓
articleApi.getPublished({ page, size })
  ↓
request.get() (自动添加Token)
  ↓
后端API返回
  ↓
更新ref状态
  ↓
ArticleCard组件渲染
```

### 文章详情加载
```
ArticleView.onMounted()
  ↓
获取route.params.slug
  ↓
articleApi.getBySlug(slug)
  ↓
Markdown渲染 + 代码高亮
  ↓
commentApi.getByArticle(id)
  ↓
渲染评论列表
```

### 用户交互
```
用户点击点赞
  ↓
检查登录状态 (useAuthStore)
  ↓
articleApi.like(id)
  ↓
更新likeCount和liked状态
  ↓
UI响应式更新
```

---

## 🔐 认证流程

### 登录
```typescript
// LoginView.vue
const authStore = useAuthStore()
await authStore.login({ username, password })
// 自动存储Token到localStorage
// 跳转到首页
```

### Token自动刷新
```
API请求 → 401错误 → request拦截器
  ↓
提取refreshToken → 调用刷新API
  ↓
获取新Token → 更新localStorage
  ↓
重发原请求
```

### 权限检查
```typescript
// 点赞按钮
if (!authStore.isAuthenticated) {
  window.alert('请先登录后点赞')
  return
}
```

---

## 🎯 路由配置

### 路由表
```typescript
const routes = [
  // 布局包裹的路由
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: '', name: 'home', component: HomeView },
      { path: 'article/:slug', name: 'article', component: ArticleView },
      { path: 'category/:id', name: 'category', component: CategoryView },
      { path: 'tag/:id', name: 'tag', component: TagView },
      { path: 'search', name: 'search', component: SearchView },
      { path: 'about', name: 'about', component: AboutView },
    ],
  },
  // 独立页面
  { path: '/login', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: RegisterView },
  // 404
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView },
]
```

### 路由守卫（可选扩展）
```typescript
// 可以添加全局守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})
```

---

## 🎨 样式系统

### Tailwind CSS配置
```css
/* styles/main.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

/* 自定义Prose样式 */
.prose {
  h1 { @apply text-3xl font-bold mb-4; }
  h2 { @apply text-2xl font-semibold mb-3; }
  p { @apply mb-4; }
  code { @apply bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded; }
  pre { @apply bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto; }
}
```

### 暗色模式
```typescript
// 使用Tailwind的dark:前缀
<div class="bg-white dark:bg-gray-900 text-gray-900 dark:text-white">
  <!-- 自动适配系统主题 -->
</div>
```

---

## 📝 组件最佳实践

### 1. 组合式API
```typescript
// ✅ 推荐：使用<script setup>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
const data = ref(null)
onMounted(() => { ... })
</script>

// ❌ 避免：Options API
export default {
  data() { return { data: null } },
  mounted() { ... }
}
```

### 2. 类型安全
```typescript
// ✅ 推荐：明确类型
const articles = ref<ArticleDTO[]>([])
const loading = ref<boolean>(false)

// ❌ 避免：隐式any
const articles = ref([])  // 类型为any[]
```

### 3. 错误处理
```typescript
// ✅ 推荐：完整错误处理
try {
  const res = await articleApi.getPublished(params)
  articles.value = res.data.content
} catch (error) {
  console.error('加载失败:', error)
  error.value = '加载失败'
} finally {
  loading.value = false
}
```

### 4. 状态管理
```typescript
// ✅ 推荐：使用Pinia
const authStore = useAuthStore()
authStore.login(data)

// ❌ 避免：全局变量
window.user = data  // 不可追踪
```

---

## 🔧 开发命令

```bash
# 启动开发服务器
npm run dev:web

# 类型检查
npm run build  # 会先执行类型检查

# 代码检查
npm run lint

# 代码格式化
npm run format
```

---

## 📦 构建输出

```bash
# 构建
npm run build

# 输出到
frontend/packages/web/dist/
  ├── index.html
  ├── assets/
  │   ├── index-abc123.js
  │   ├── index-xyz456.css
  │   └── ...
```

**单体部署**: 构建后的文件会被复制到后端的`src/main/resources/static/`目录

---

## 🧪 测试策略

### 组件测试
```typescript
// 使用Vitest + Vue Test Utils
import { mount } from '@vue/test-utils'
import ArticleCard from './ArticleCard.vue'

test('renders article title', () => {
  const wrapper = mount(ArticleCard, {
    props: {
      article: { title: 'Test', ... }
    }
  })
  expect(wrapper.text()).toContain('Test')
})
```

### E2E测试
```typescript
// 使用Playwright或Cypress
test('user can login and like article', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'admin')
  await page.fill('input[name="password"]', 'admin123')
  await page.click('button[type="submit"]')

  await page.goto('/article/test')
  await page.click('button:has-text("点赞")')

  await expect(page.locator('text=11')).toBeVisible()
})
```

---

## 📊 性能优化

### 1. 懒加载
```typescript
// 路由懒加载
const ArticleView = () => import('@/views/ArticleView.vue')
```

### 2. 图片优化
```vue
<img
  :src="article.coverImage"
  :alt="article.title"
  loading="lazy"  <!-- 懒加载 -->
/>
```

### 3. 虚拟滚动（大数据）
```typescript
// 如果文章列表很长，可以使用虚拟滚动
import { useVirtualList } from '@vueuse/core'
```

### 4. 缓存策略
```typescript
// 可以添加简单的缓存
const cache = new Map()
async function loadArticle(slug: string) {
  if (cache.has(slug)) {
    return cache.get(slug)
  }
  const data = await articleApi.getBySlug(slug)
  cache.set(slug, data)
  return data
}
```

---

## 🎯 核心特性总结

| 功能 | 实现 | 状态 |
|------|------|------|
| 文章列表 | 分页 + 加载更多 | ✅ |
| 文章详情 | Markdown渲染 | ✅ |
| 代码高亮 | highlight.js | ✅ |
| 搜索功能 | 关键词搜索 | ✅ |
| 点赞功能 | 需登录 | ✅ |
| 评论功能 | 登录/匿名 | ✅ |
| 分类浏览 | 路由参数 | ✅ |
| 标签浏览 | 路由参数 | ✅ |
| 用户认证 | JWT + Pinia | ✅ |
| Token刷新 | 自动 | ✅ |
| 响应式 | Tailwind | ✅ |
| 暗色模式 | Tailwind dark | ✅ |

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队