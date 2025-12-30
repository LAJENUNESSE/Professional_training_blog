<script setup lang="ts">
  import { ref, onMounted, computed } from 'vue'
  import { useRouter, useRoute } from 'vue-router'
  import {
    NCard,
    NForm,
    NFormItem,
    NInput,
    NSelect,
    NSwitch,
    NButton,
    NSpace,
    useMessage,
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

  const tagOptions = computed(() => tags.value.map((t) => ({ label: t.name, value: t.id })))

  async function loadData() {
    try {
      const [catRes, tagRes] = await Promise.all([
        categoryApi.admin.getAll() as unknown as Promise<{ data: CategoryDTO[] }>,
        tagApi.admin.getAll() as unknown as Promise<{ data: TagDTO[] }>,
      ])
      categories.value = catRes.data
      tags.value = tagRes.data

      if (isEdit.value) {
        loading.value = true
        const res = (await articleApi.admin.getById(Number(route.params.id))) as unknown as {
          data: ArticleDTO
        }
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
        const res = (await uploadApi.uploadImage(file)) as unknown as {
          data: { url: string }
        }
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
      <!-- Main Content -->
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

      <!-- Sidebar -->
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
