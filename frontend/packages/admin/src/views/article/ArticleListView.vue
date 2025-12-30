<script setup lang="ts">
  import { ref, onMounted, h, watch, computed } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import {
    NCard,
    NDataTable,
    NButton,
    NSpace,
    NTag,
    NPopconfirm,
    useMessage,
  } from 'naive-ui'
  import type { DataTableColumns } from 'naive-ui'
  import { articleApi, type ArticleDTO, type PageResult, formatDate, ARTICLE_STATUS } from '@blog/shared'

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
          h(
            NButton,
            { size: 'small', onClick: () => router.push(`/articles/${row.id}/edit`) },
            () => '编辑'
          ),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除'),
              default: () => '确定删除该文章？',
            }
          ),
        ]),
    },
  ]

  async function loadData() {
    loading.value = true
    try {
      const res = (await articleApi.admin.getAll({
        page: pagination.value.page - 1,
        size: pagination.value.pageSize,
        categoryId: filterCategoryId.value ?? undefined,
        tagId: filterTagId.value ?? undefined,
      })) as unknown as { data: PageResult<ArticleDTO> }
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
