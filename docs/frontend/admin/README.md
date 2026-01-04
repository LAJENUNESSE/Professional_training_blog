# 后台管理应用 (@blog/admin)

## 📋 概述

`@blog/admin` 是博客系统的后台管理界面，提供完整的CRUD操作和数据管理功能。

**访问地址**: http://localhost:3001/admin
**默认账号**: admin / admin123

## 🏗️ 项目结构

```
frontend/packages/admin/
├── src/
│   ├── main.ts              # 应用入口
│   ├── App.vue              # 根组件
│   ├── router/              # 路由配置
│   │   └── index.ts
│   ├── stores/              # Pinia状态管理
│   │   ├── index.ts
│   │   └── theme.ts         # 主题Store
│   ├── views/               # 页面组件
│   │   ├── LoginView.vue    # 登录页
│   │   ├── DashboardView.vue # 仪表盘
│   │   ├── article/         # 文章管理
│   │   │   ├── ArticleListView.vue
│   │   │   └── ArticleEditView.vue
│   │   ├── category/        # 分类管理
│   │   ├── tag/             # 标签管理
│   │   ├── comment/         # 评论管理
│   │   ├── user/            # 用户管理
│   │   └── setting/         # 设置管理
│   ├── components/          # 通用组件
│   │   ├── layout/
│   │   │   └── AdminLayout.vue  # 管理后台布局
│   │   └── DataTable.vue        # 数据表格封装
│   ├── styles/              # 样式文件
│   └── assets/              # 静态资源
├── vite.config.ts
├── package.json
└── tsconfig.json
```

## 📚 核心页面详解

### 1. 登录页 (LoginView.vue)

**功能**: 管理员登录认证

**核心代码**:
```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, useMessage } from 'naive-ui'
import { useAuthStore, type LoginRequest } from '@blog/shared'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const authStore = useAuthStore()

const formValue = ref<LoginRequest>({
  username: '',
  password: '',
})

const loading = ref(false)

async function handleLogin() {
  if (!formValue.value.username || !formValue.value.password) {
    message.warning('请填写用户名和密码')
    return
  }

  loading.value = true
  try {
    await authStore.login(formValue.value)

    // 检查是否是管理员
    if (!authStore.isAdmin) {
      message.error('需要管理员权限')
      authStore.logout()
      return
    }

    message.success('登录成功')

    // 跳转到目标页面或仪表盘
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } catch (err) {
    message.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
    <NCard class="w-96" title="管理员登录">
      <NForm :model="formValue" label-placement="top">
        <NFormItem label="用户名" required>
          <NInput
            v-model:value="formValue.username"
            placeholder="请输入用户名"
            @keyup.enter="handleLogin"
          />
        </NFormItem>
        <NFormItem label="密码" required>
          <NInput
            v-model:value="formValue.password"
            type="password"
            placeholder="请输入密码"
            @keyup.enter="handleLogin"
          />
        </NFormItem>
        <NButton
          type="primary"
          :loading="loading"
          :block="true"
          @click="handleLogin"
        >
          登录
        </NButton>
      </NForm>
    </NCard>
  </div>
</template>
```

**特性**:
- ✅ 表单验证
- ✅ 权限检查（必须是ADMIN）
- ✅ 登录后跳转
- ✅ Enter键提交

---

### 2. 仪表盘 (DashboardView.vue)

**功能**: 数据统计、最近文章概览

**核心代码**:
```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { NGrid, NGi, NCard, NStatistic, NNumberAnimation } from 'naive-ui'
import {
  articleApi,
  commentApi,
  categoryApi,
  tagApi,
  type ArticleDTO,
  type PageResult,
} from '@blog/shared'

const stats = ref({
  articles: 0,
  categories: 0,
  tags: 0,
  pendingComments: 0,
})

const recentArticles = ref<ArticleDTO[]>([])

onMounted(async () => {
  try {
    const [articlesRes, categoriesRes, tagsRes, pendingRes] = await Promise.all([
      articleApi.admin.getAll({ page: 0, size: 1 }),
      categoryApi.admin.getAll(),
      tagApi.admin.getAll(),
      commentApi.admin.getPendingCount(),
    ])

    stats.value = {
      articles: articlesRes.data.totalElements,
      categories: categoriesRes.data.length,
      tags: tagsRes.data.length,
      pendingComments: pendingRes.data,
    }

    const recentRes = await articleApi.admin.getAll({ page: 0, size: 5 })
    recentArticles.value = recentRes.data.content
  } catch (err) {
    console.error('Failed to load stats:', err)
  }
})
</script>

<template>
  <div class="space-y-6">
    <h1 class="text-2xl font-bold">仪表盘</h1>

    <!-- 统计卡片 -->
    <NGrid :x-gap="16" :y-gap="16" :cols="4">
      <NGi>
        <NCard>
          <NStatistic label="文章总数">
            <NNumberAnimation :from="0" :to="stats.articles" />
          </NStatistic>
        </NCard>
      </NGi>
      <NGi>
        <NCard>
          <NStatistic label="分类数量">
            <NNumberAnimation :from="0" :to="stats.categories" />
          </NStatistic>
        </NCard>
      </NGi>
      <NGi>
        <NCard>
          <NStatistic label="标签数量">
            <NNumberAnimation :from="0" :to="stats.tags" />
          </NStatistic>
        </NCard>
      </NGi>
      <NGi>
        <NCard>
          <NStatistic label="待审核评论">
            <NNumberAnimation :from="0" :to="stats.pendingComments" />
          </NStatistic>
        </NCard>
      </NGi>
    </NGrid>

    <!-- 最近文章 -->
    <NCard title="最近文章">
      <div v-if="recentArticles.length === 0" class="text-gray-500">暂无文章</div>
      <div v-else class="space-y-3">
        <div
          v-for="article in recentArticles"
          :key="article.id"
          class="flex items-center justify-between py-2 border-b border-gray-100 last:border-0"
        >
          <RouterLink
            :to="`/articles/${article.id}/edit`"
            class="text-blue-600 hover:text-blue-700"
          >
            {{ article.title }}
          </RouterLink>
          <span class="text-sm text-gray-500">{{ article.status }}</span>
        </div>
      </div>
    </NCard>
  </div>
</template>
```

**特性**:
- ✅ 并行加载数据
- ✅ 数字动画效果
- ✅ 快速跳转编辑

---

### 3. 文章编辑 (ArticleEditView.vue)

**功能**: 创建/编辑文章，Markdown编辑器，图片上传

**核心代码**:
```vue
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NCard, NForm, NFormItem, NInput, NSelect,
  NSwitch, NButton, NSpace, useMessage,
} from 'naive-ui'
import { Editor } from '@bytemd/vue-next'
import gfm from '@bytemd/plugin-gfm'
import highlight from '@bytemd/plugin-highlight'
import 'bytemd/dist/index.css'
import 'highlight.js/styles/github.css'
import {
  articleApi,
  categoryApi,
  tagApi,
  uploadApi,
  type ArticleDTO,
  type CategoryDTO,
  type TagDTO,
  type ArticleRequest,
} from '@blog/shared'

const router = useRouter()
const route = useRoute()
const message = useMessage()

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const categories = ref<CategoryDTO[]>([])
const tags = ref<TagDTO[]>([])

const plugins = [gfm(), highlight()]

const formValue = ref<ArticleRequest>({
  title: '',
  slug: '',
  summary: '',
  content: '',
  coverImage: '',
  status: 'DRAFT',
  isTop: false,
  allowComment: true,
  categoryId: undefined,
  tagIds: [],
})

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '发布', value: 'PUBLISHED' },
  { label: '归档', value: 'ARCHIVED' },
]

const categoryOptions = computed(() =>
  categories.value.map((c) => ({ label: c.name, value: c.id }))
)

const tagOptions = computed(() =>
  tags.value.map((t) => ({ label: t.name, value: t.id }))
)

async function loadData() {
  try {
    const [catRes, tagRes] = await Promise.all([
      categoryApi.admin.getAll(),
      tagApi.admin.getAll(),
    ])
    categories.value = catRes.data
    tags.value = tagRes.data

    if (isEdit.value) {
      loading.value = true
      const res = await articleApi.admin.getById(Number(route.params.id))
      const article = res.data
      formValue.value = {
        title: article.title,
        slug: article.slug,
        summary: article.summary || '',
        content: article.content,
        coverImage: article.coverImage || '',
        status: article.status,
        isTop: article.isTop,
        allowComment: article.allowComment,
        categoryId: article.category?.id,
        tagIds: article.tags.map((t) => t.id),
      }
    }
  } catch (err) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!formValue.value.title || !formValue.value.content) {
    message.warning('请填写标题和内容')
    return
  }

  if (formValue.value.status === 'PUBLISHED' && !formValue.value.slug?.trim()) {
    message.warning('发布文章时必须填写 Slug')
    return
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await articleApi.admin.update(Number(route.params.id), formValue.value)
      message.success('更新成功')
    } else {
      await articleApi.admin.create(formValue.value)
      message.success('创建成功')
    }
    router.push('/articles')
  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : '保存失败'
    message.error(errorMsg)
    console.error('Save article error:', err)
  } finally {
    saving.value = false
  }
}

async function handleUploadImages(files: File[]): Promise<{ url: string }[]> {
  const results: { url: string }[] = []
  for (const file of files) {
    try {
      const res = await uploadApi.uploadImage(file)
      results.push({ url: res.data.url })
    } catch (err) {
      message.error('图片上传失败')
    }
  }
  return results
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">{{ isEdit ? '编辑文章' : '新建文章' }}</h1>
      <NSpace>
        <NButton @click="router.back()">取消</NButton>
        <NButton type="primary" :loading="saving" @click="handleSave">保存</NButton>
      </NSpace>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <!-- 主内容区 -->
      <div class="lg:col-span-3 space-y-4">
        <NCard class="h-full">
          <NForm :model="formValue" label-placement="top">
            <NFormItem label="标题" required>
              <NInput v-model:value="formValue.title" placeholder="请输入文章标题" />
            </NFormItem>
            <NFormItem label="内容" required>
              <Editor
                :value="formValue.content"
                :plugins="plugins"
                :upload-images="handleUploadImages"
                :style="{ minHeight: '70vh' }"
                @change="(v: string) => (formValue.content = v)"
              />
            </NFormItem>
          </NForm>
        </NCard>
      </div>

      <!-- 侧边栏 -->
      <div class="lg:col-span-1 space-y-4">
        <NCard title="发布设置" class="sticky top-4">
          <NForm :model="formValue" label-placement="top" size="small">
            <NFormItem label="状态">
              <NSelect v-model:value="formValue.status" :options="statusOptions" />
            </NFormItem>
            <NFormItem label="分类">
              <NSelect
                v-model:value="formValue.categoryId"
                :options="categoryOptions"
                clearable
                placeholder="选择分类"
              />
            </NFormItem>
            <NFormItem label="标签">
              <NSelect
                v-model:value="formValue.tagIds"
                :options="tagOptions"
                multiple
                placeholder="选择标签"
              />
            </NFormItem>
            <NFormItem label="置顶">
              <NSwitch v-model:value="formValue.isTop" />
            </NFormItem>
            <NFormItem label="允许评论">
              <NSwitch v-model:value="formValue.allowComment" />
            </NFormItem>
          </NForm>
        </NCard>

        <NCard title="SEO 设置">
          <NForm :model="formValue" label-placement="top" size="small">
            <NFormItem label="Slug" :required="formValue.status === 'PUBLISHED'">
              <NInput v-model:value="formValue.slug" placeholder="URL 别名（发布时必填）" />
            </NFormItem>
            <NFormItem label="摘要">
              <NInput
                v-model:value="formValue.summary"
                type="textarea"
                :rows="3"
                placeholder="文章摘要"
              />
            </NFormItem>
            <NFormItem label="封面图">
              <NInput v-model:value="formValue.coverImage" placeholder="封面图 URL" />
            </NFormItem>
          </NForm>
        </NCard>
      </div>
    </div>
  </div>
</template>
```

**核心功能**:

#### ByteMD编辑器
```typescript
import { Editor } from '@bytemd/vue-next'
import gfm from '@bytemd/plugin-gfm'  // GitHub风格Markdown
import highlight from '@bytemd/plugin-highlight'  // 代码高亮

const plugins = [gfm(), highlight()]

<Editor
  :value="formValue.content"
  :plugins="plugins"
  :upload-images="handleUploadImages"
  @change="(v) => formValue.content = v"
/>
```

#### 图片上传
```typescript
async function handleUploadImages(files: File[]): Promise<{ url: string }[]> {
  const results = []
  for (const file of files) {
    const res = await uploadApi.uploadImage(file)
    results.push({ url: res.data.url })
  }
  return results
}
```

**特性**:
- ✅ Markdown实时预览
- ✅ 代码语法高亮
- ✅ 拖拽上传图片
- ✅ 分类/标签选择
- ✅ 置顶/评论开关
- ✅ SEO设置（Slug、摘要、封面）
- ✅ 发布状态管理

---

### 4. 文章列表 (ArticleListView.vue)

**功能**: 文章管理表格，支持分页、筛选、删除

**核心代码**:
```vue
<script setup lang="ts">
import { ref, onMounted, h, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NCard, NDataTable, NButton, NSpace, NTag, NPopconfirm, useMessage,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  articleApi,
  type ArticleDTO,
  type PageResult,
  formatDate,
  ARTICLE_STATUS,
} from '@blog/shared'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const loading = ref(false)
const articles = ref<ArticleDTO[]>([])
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

const filterCategoryId = ref<number | null>(
  route.query.categoryId ? Number(route.query.categoryId) : null
)
const filterTagId = ref<number | null>(route.query.tagId ? Number(route.query.tagId) : null)

const filterText = computed(() => {
  if (filterCategoryId.value) {
    return `分类ID: ${filterCategoryId.value}`
  }
  if (filterTagId.value) {
    return `标签ID: ${filterTagId.value}`
  }
  return ''
})

const columns: DataTableColumns<ArticleDTO> = [
  { title: 'ID', key: 'id', width: 60 },
  {
    title: '标题',
    key: 'title',
    ellipsis: { tooltip: true },
  },
  {
    title: '分类',
    key: 'category',
    width: 100,
    render: (row) => row.category?.name || '-',
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => {
      const types: Record<string, 'success' | 'warning' | 'default'> = {
        PUBLISHED: 'success',
        DRAFT: 'warning',
        ARCHIVED: 'default',
      }
      return h(NTag, { type: types[row.status] || 'default', size: 'small' }, () => ARTICLE_STATUS[row.status])
    },
  },
  {
    title: '阅读量',
    key: 'viewCount',
    width: 80,
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: 120,
    render: (row) => formatDate(row.createdAt),
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: (row) =>
      h(NSpace, null, () => [
        h(NButton, {
          size: 'small',
          onClick: () => router.push(`/articles/${row.id}/edit`)
        }, () => '编辑'),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row.id)
        }, {
          trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除'),
          default: () => '确定删除该文章？',
        }),
      ]),
  },
]

async function loadData() {
  loading.value = true
  try {
    const res = await articleApi.admin.getAll({
      page: pagination.value.page - 1,
      size: pagination.value.pageSize,
      categoryId: filterCategoryId.value ?? undefined,
      tagId: filterTagId.value ?? undefined,
    })
    articles.value = res.data.content
    pagination.value.itemCount = res.data.totalElements
  } catch (err) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.value.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadData()
}

function clearFilter() {
  filterCategoryId.value = null
  filterTagId.value = null
  router.replace({ path: '/articles', query: {} })
  loadData()
}

async function handleDelete(id: number) {
  try {
    await articleApi.admin.delete(id)
    message.success('删除成功')
    loadData()
  } catch (err) {
    message.error('删除失败')
  }
}

onMounted(loadData)

watch(
  () => route.query,
  (q) => {
    filterCategoryId.value = q.categoryId ? Number(q.categoryId) : null
    filterTagId.value = q.tagId ? Number(q.tagId) : null
    loadData()
  }
)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">文章管理</h1>
      <NButton type="primary" @click="router.push('/articles/new')">
        新建文章
      </NButton>
    </div>

    <div v-if="filterText" class="flex items-center gap-3 text-sm text-gray-600">
      <span>当前筛选：{{ filterText }}</span>
      <NButton secondary size="small" @click="clearFilter">清除</NButton>
    </div>

    <NCard>
      <NDataTable
        :columns="columns"
        :data="articles"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: ArticleDTO) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </NCard>
  </div>
</template>
```

**特性**:
- ✅ 分页表格
- ✅ 状态标签（颜色区分）
- ✅ 操作按钮（编辑/删除）
- ✅ 确认删除弹窗
- ✅ URL参数筛选

---

### 5. 评论管理 (CommentView.vue)

**功能**: 评论审核、回复、删除

**核心代码**:
```vue
<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import {
  NCard, NDataTable, NButton, NSpace, NTag, NSelect, NPopconfirm, useMessage,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  commentApi,
  type CommentDTO,
  type PageResult,
  formatDate,
  COMMENT_STATUS,
} from '@blog/shared'

const message = useMessage()
const loading = ref(false)
const comments = ref<CommentDTO[]>([])
const statusFilter = ref<string>('')
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
]

const columns: DataTableColumns<CommentDTO> = [
  { title: 'ID', key: 'id', width: 60 },
  {
    title: '内容',
    key: 'content',
    ellipsis: { tooltip: true },
  },
  {
    title: '文章',
    key: 'articleTitle',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: '作者',
    key: 'authorName',
    width: 100,
    render: (row) => row.authorName || '匿名',
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const types: Record<string, 'success' | 'warning' | 'error'> = {
        APPROVED: 'success',
        PENDING: 'warning',
        REJECTED: 'error',
      }
      return h(NTag, { type: types[row.status], size: 'small' }, () => COMMENT_STATUS[row.status])
    },
  },
  {
    title: '时间',
    key: 'createdAt',
    width: 120,
    render: (row) => formatDate(row.createdAt),
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) =>
      h(NSpace, null, () => [
        row.status === 'PENDING' &&
          h(NButton, { size: 'small', type: 'success', onClick: () => handleApprove(row.id) }, () => '通过'),
        row.status === 'PENDING' &&
          h(NButton, { size: 'small', type: 'warning', onClick: () => handleReject(row.id) }, () => '拒绝'),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row.id)
        }, {
          trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除'),
          default: () => '确定删除该评论？',
        }),
      ]),
  },
]

async function loadData() {
  loading.value = true
  try {
    const res = await commentApi.admin.getAll({
      page: pagination.value.page - 1,
      size: pagination.value.pageSize,
      status: statusFilter.value || undefined,
    })
    comments.value = res.data.content
    pagination.value.itemCount = res.data.totalElements
  } catch (err) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.value.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadData()
}

function handleStatusChange() {
  pagination.value.page = 1
  loadData()
}

async function handleApprove(id: number) {
  try {
    await commentApi.admin.approve(id)
    message.success('审核通过')
    loadData()
  } catch (err) {
    message.error('操作失败')
  }
}

async function handleReject(id: number) {
  try {
    await commentApi.admin.reject(id)
    message.success('已拒绝')
    loadData()
  } catch (err) {
    message.error('操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await commentApi.admin.delete(id)
    message.success('删除成功')
    loadData()
  } catch (err) {
    message.error('删除失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">评论管理</h1>
      <NSelect
        v-model:value="statusFilter"
        :options="statusOptions"
        style="width: 120px"
        @update:value="handleStatusChange"
      />
    </div>

    <NCard>
      <NDataTable
        :columns="columns"
        :data="comments"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: CommentDTO) => row.id"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </NCard>
  </div>
</template>
```

**审核流程**:
```
待审核评论 → 通过/拒绝 → 更新状态 → 列表刷新
```

---

## 🎨 布局组件

### AdminLayout
**位置**: `components/layout/AdminLayout.vue`

**结构**:
```vue
<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- 侧边栏 -->
    <aside class="fixed left-0 top-0 h-full w-64 bg-white dark:bg-gray-800 border-r">
      <div class="p-4 border-b">
        <h2 class="text-xl font-bold">博客管理</h2>
      </div>
      <nav class="p-2">
        <NMenu :options="menuOptions" :value="currentRoute" />
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="ml-64 p-6">
      <header class="mb-6 flex justify-between items-center">
        <div>
          <h1 class="text-2xl font-bold">{{ pageTitle }}</h1>
        </div>
        <div class="flex items-center gap-3">
          <span>{{ authStore.user?.nickname || authStore.user?.username }}</span>
          <NButton @click="handleLogout">退出</NButton>
        </div>
      </header>

      <router-view />
    </main>
  </div>
</template>
```

**特性**:
- ✅ 固定侧边栏
- ✅ 面包屑导航
- ✅ 用户信息显示
- ✅ 退出登录

---

## 🛠️ 技术栈

### 核心依赖
```json
{
  "vue": "^3.5.13",
  "vue-router": "^4.5.0",
  "pinia": "^2.3.0",
  "naive-ui": "^2.41.0",
  "@blog/shared": "workspace:^",
  "@bytemd/vue-next": "^1.21.0",
  "bytemd": "^1.21.0",
  "@bytemd/plugin-gfm": "^1.21.0",
  "@bytemd/plugin-highlight": "^1.21.0"
}
```

### 开发工具
```json
{
  "vite": "^6.0.5",
  "vue-tsc": "^2.2.0",
  "tailwindcss": "^3.4.17",
  "unplugin-auto-import": "^0.18.6",
  "unplugin-vue-components": "^0.27.5",
  "unplugin-icons": "^0.21.0"
}
```

---

## 📊 数据流

### 文章CRUD流程
```
列表页 → 新建/编辑 → 表单提交 → API调用 → 后端处理 → 返回结果 → 列表刷新
```

### 评论审核流程
```
待审核列表 → 点击通过/拒绝 → API调用 → 更新状态 → 列表刷新
```

### 权限验证流程
```
访问页面 → 路由守卫 → 检查登录状态 → 检查ADMIN角色 → 允许/拒绝
```

---

## 🔐 路由守卫

### 认证检查
```typescript
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  // 需要登录且未登录
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  // 登录页已登录
  if (to.name === 'login' && authStore.isAuthenticated) {
    next({ name: 'dashboard' })
    return
  }

  next()
})
```

### 管理员权限检查
```typescript
// 在组件中
if (!authStore.isAdmin) {
  message.error('需要管理员权限')
  router.back()
}
```

---

## 🎯 核心功能对比

| 功能 | 前台 (@blog/web) | 后台 (@blog/admin) |
|------|------------------|-------------------|
| **UI库** | Tailwind CSS | Naive UI + Tailwind |
| **编辑器** | 仅展示（Markdown渲染） | ByteMD（完整编辑） |
| **数据操作** | 只读 + 评论/点赞 | 完整CRUD |
| **权限** | 公开/登录 | 仅ADMIN |
| **路由** | `/` 前缀 | `/admin/` 前缀 |
| **布局** | 简单布局 | 侧边栏管理布局 |
| **交互** | 面向访客 | 面向管理员 |

---

## 📝 最佳实践

### 1. 表单验证
```typescript
// ✅ 推荐：提前验证
if (!formValue.value.title || !formValue.value.content) {
  message.warning('请填写标题和内容')
  return
}

// ✅ 业务规则验证
if (formValue.value.status === 'PUBLISHED' && !formValue.value.slug?.trim()) {
  message.warning('发布文章时必须填写 Slug')
  return
}
```

### 2. 并行加载
```typescript
// ✅ 推荐：Promise.all
const [catRes, tagRes] = await Promise.all([
  categoryApi.admin.getAll(),
  tagApi.admin.getAll(),
])
```

### 3. 错误处理
```typescript
// ✅ 推荐：完整错误处理
try {
  await apiCall()
  message.success('成功')
} catch (err) {
  const msg = err instanceof Error ? err.message : '操作失败'
  message.error(msg)
} finally {
  loading.value = false
}
```

### 4. 确认操作
```typescript
// ✅ 推荐：危险操作确认
<NPopconfirm @positiveClick="handleDelete">
  <template #trigger>
    <NButton type="error">删除</NButton>
  </template>
  确定删除该文章？
</NPopconfirm>
```

---

## 🔧 开发命令

```bash
# 启动开发服务器
npm run dev:admin

# 类型检查
npm run build

# 代码检查
npm run lint

# 代码格式化
npm run format
```

---

## 📦 构建部署

### 开发环境
```bash
# 端口：3001
# 访问：http://localhost:3001/admin
```

### 生产环境（单体部署）
```bash
# 1. 构建前端
npm run build:admin

# 2. 复制到后端
cp -r packages/admin/dist/* ../backend/src/main/resources/static/admin/

# 3. 后端打包
cd ../backend && ./mvnw package

# 4. 运行
java -jar target/blog-0.0.1-SNAPSHOT.jar

# 5. 访问
http://localhost:8080/admin
```

---

## 🎨 UI组件库优势

### Naive UI特点
1. **企业级**: 适合管理后台
2. **TypeScript友好**: 完整类型支持
3. **主题定制**: 支持暗色模式
4. **组件丰富**: 表格、表单、弹窗等

### 常用组件
```typescript
import {
  NCard,           // 卡片
  NForm,           // 表单
  NInput,          // 输入框
  NSelect,         // 下拉选择
  NSwitch,         // 开关
  NButton,         // 按钮
  NDataTable,      // 数据表格
  NPopconfirm,     // 确认弹窗
  useMessage,      // 消息提示
} from 'naive-ui'
```

---

## 📊 功能矩阵

| 页面 | 功能 | 操作 |
|------|------|------|
| **仪表盘** | 数据统计、最近文章 | 查看 |
| **文章列表** | 分页、筛选、删除 | CRUD |
| **文章编辑** | 创建/编辑、Markdown、上传图片 | CRUD |
| **分类管理** | 增删改查 | CRUD |
| **标签管理** | 增删改查 | CRUD |
| **评论管理** | 审核、拒绝、删除 | 审核 |
| **用户管理** | 查看用户列表 | 查看 |
| **设置管理** | 系统配置 | 编辑 |

---

## 🔍 调试技巧

### 1. 查看API请求
```typescript
// 在request.ts中添加日志
request.interceptors.request.use((config) => {
  console.log('Request:', config.method, config.url)
  return config
})
```

### 2. 检查权限
```typescript
// 在组件中
console.log('User:', authStore.user)
console.log('Is Admin:', authStore.isAdmin)
```

### 3. 查看Naive UI主题
```typescript
// 在main.ts中
import { createTheme } from 'naive-ui'
const theme = createTheme(...)  // 自定义主题
```

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队