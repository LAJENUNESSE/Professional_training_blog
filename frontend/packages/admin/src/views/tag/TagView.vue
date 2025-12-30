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
    NPopconfirm,
    useMessage,
  } from 'naive-ui'
  import type { DataTableColumns } from 'naive-ui'
  import { tagApi, type TagDTO, type TagRequest, formatDate } from '@blog/shared'
  import { useRouter } from 'vue-router'

  const router = useRouter()
  const message = useMessage()
  const loading = ref(false)
  const tags = ref<TagDTO[]>([])
  const showModal = ref(false)
  const modalLoading = ref(false)
  const editingId = ref<number | null>(null)

  const formValue = ref<TagRequest>({
    name: '',
    slug: '',
  })

  const columns: DataTableColumns<TagDTO> = [
    { title: 'ID', key: 'id', width: 60 },
    { title: '名称', key: 'name' },
    { title: 'Slug', key: 'slug' },
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
              default: () => '确定删除该标签？',
            }
          ),
        ]),
    },
  ]

  async function loadData() {
    loading.value = true
    try {
      const res = (await tagApi.admin.getAll()) as unknown as { data: TagDTO[] }
      tags.value = res.data
    } catch (err) {
      message.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  function handleAdd() {
    editingId.value = null
    formValue.value = { name: '', slug: '' }
    showModal.value = true
  }

  function handleEdit(tag: TagDTO) {
    editingId.value = tag.id
    formValue.value = { name: tag.name, slug: tag.slug }
    showModal.value = true
  }

  async function handleSave() {
    if (!formValue.value.name) {
      message.warning('请输入标签名称')
      return
    }

    modalLoading.value = true
    try {
      if (editingId.value) {
        await tagApi.admin.update(editingId.value, formValue.value)
        message.success('更新成功')
      } else {
        await tagApi.admin.create(formValue.value)
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
      await tagApi.admin.delete(id)
      message.success('删除成功')
      loadData()
    } catch (err) {
      message.error('删除失败')
    }
  }

  function handleViewArticles(id: number) {
    router.push({ path: '/articles', query: { tagId: id } })
  }

  onMounted(loadData)
</script>

<template>
  <div class="space-y-4">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold">标签管理</h1>
      <NButton type="primary" @click="handleAdd">新建标签</NButton>
    </div>

    <NCard>
      <NDataTable
        :columns="columns"
        :data="tags"
        :loading="loading"
        :row-key="(row: TagDTO) => row.id"
      />
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="dialog"
      :title="editingId ? '编辑标签' : '新建标签'"
      positive-text="保存"
      negative-text="取消"
      :positive-button-props="{ loading: modalLoading }"
      @positive-click="handleSave"
    >
      <NForm :model="formValue" label-placement="left" label-width="80">
        <NFormItem label="名称" required>
          <NInput v-model:value="formValue.name" placeholder="标签名称" />
        </NFormItem>
        <NFormItem label="Slug">
          <NInput v-model:value="formValue.slug" placeholder="URL 别名" />
        </NFormItem>
      </NForm>
    </NModal>
  </div>
</template>
