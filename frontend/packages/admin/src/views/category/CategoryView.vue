<script setup lang="ts">
  import { ref, onMounted, h } from 'vue'
  import {
    NCard,
    NDataTable,
    NButton,
    NSpace,
    NModal,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NPopconfirm,
    useMessage,
  } from 'naive-ui'
  import type { DataTableColumns } from 'naive-ui'
  import { categoryApi, type CategoryDTO, type CategoryRequest, formatDate } from '@blog/shared'
  import { useRouter } from 'vue-router'

  const router = useRouter()
  const message = useMessage()
  const loading = ref(false)
  const categories = ref<CategoryDTO[]>([])
  const showModal = ref(false)
  const modalLoading = ref(false)
  const editingId = ref<number | null>(null)

  const formValue = ref<CategoryRequest>({
    name: '',
    slug: '',
    description: '',
    sortOrder: 0,
  })

  const columns: DataTableColumns<CategoryDTO> = [
    { title: 'ID', key: 'id', width: 60 },
    { title: '名称', key: 'name' },
    { title: 'Slug', key: 'slug' },
    { title: '描述', key: 'description', ellipsis: { tooltip: true } },
    { title: '排序', key: 'sortOrder', width: 80 },
    { title: '文章数', key: 'articleCount', width: 80 },
    {
      title: '创建时间',
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
          h(
            NButton,
            { size: 'small', tertiary: true, onClick: () => handleViewArticles(row.id) },
            () => '查看文章'
          ),
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, () => '编辑'),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, () => '删除'),
              default: () => '确定删除该分类？',
            }
          ),
        ]),
    },
  ]

  async function loadData() {
    loading.value = true
    try {
      const res = (await categoryApi.admin.getAll()) as unknown as { data: CategoryDTO[] }
      categories.value = res.data
    } catch (err) {
      message.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  function handleAdd() {
    editingId.value = null
    formValue.value = { name: '', slug: '', description: '', sortOrder: 0 }
    showModal.value = true
  }

  function handleEdit(category: CategoryDTO) {
    editingId.value = category.id
    formValue.value = {
      name: category.name,
      slug: category.slug,
      description: category.description || '',
      sortOrder: category.sortOrder,
    }
    showModal.value = true
  }

  async function handleSave() {
    if (!formValue.value.name) {
      message.warning('请输入分类名称')
      return
    }

    modalLoading.value = true
    try {
      if (editingId.value) {
        await categoryApi.admin.update(editingId.value, formValue.value)
        message.success('更新成功')
      } else {
        await categoryApi.admin.create(formValue.value)
        message.success('创建成功')
      }
      showModal.value = false
      loadData()
    } catch (err) {
      message.error('保存失败')
    } finally {
      modalLoading.value = false
    }
  }

  async function handleDelete(id: number) {
    try {
      await categoryApi.admin.delete(id)
      message.success('删除成功')
      loadData()
    } catch (err) {
      message.error('删除失败')
    }
  }

  function handleViewArticles(id: number) {
    router.push({ path: '/articles', query: { categoryId: id } })
  }

  onMounted(loadData)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">分类管理</h1>
      <NButton type="primary" @click="handleAdd">新建分类</NButton>
    </div>

    <NCard>
      <NDataTable
        :columns="columns"
        :data="categories"
        :loading="loading"
        :row-key="(row: CategoryDTO) => row.id"
      />
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="dialog"
      :title="editingId ? '编辑分类' : '新建分类'"
      positive-text="保存"
      negative-text="取消"
      :positive-button-props="{ loading: modalLoading }"
      @positive-click="handleSave"
    >
      <NForm :model="formValue" label-placement="left" label-width="80">
        <NFormItem label="名称" required>
          <NInput v-model:value="formValue.name" placeholder="分类名称" />
        </NFormItem>
        <NFormItem label="Slug">
          <NInput v-model:value="formValue.slug" placeholder="URL 别名" />
        </NFormItem>
        <NFormItem label="描述">
          <NInput v-model:value="formValue.description" type="textarea" placeholder="分类描述" />
        </NFormItem>
        <NFormItem label="排序">
          <NInputNumber v-model:value="formValue.sortOrder" :min="0" />
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>
