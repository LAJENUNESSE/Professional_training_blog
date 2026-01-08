<script setup lang="ts">
  import { ref, onMounted, watch } from 'vue'
  import { RouterLink, useRoute } from 'vue-router'
  import { articleApi, type ArticleDTO, type ArticleSuggestionDTO, type PageResult } from '@blog/shared'
  import ArticleCard from '@/components/article/ArticleCard.vue'

  const route = useRoute()
  const keyword = ref('')
  const articles = ref<ArticleDTO[]>([])
  const suggestions = ref<ArticleSuggestionDTO[]>([])
  const suggestionLoading = ref(false)
  const loading = ref(false)
  const currentPage = ref(0)
  const hasMore = ref(false)
  const total = ref(0)

  async function loadSuggestions() {
    if (!keyword.value.trim()) return

    suggestionLoading.value = true
    try {
      const res = (await articleApi.suggest(keyword.value, 5)) as unknown as {
        data: ArticleSuggestionDTO[]
      }
      suggestions.value = res.data
    } catch (error) {
      console.error('Failed to load suggestions:', error)
      suggestions.value = []
    } finally {
      suggestionLoading.value = false
    }
  }

  async function search(page = 0) {
    if (!keyword.value.trim()) return

    loading.value = true
    try {
      const res = (await articleApi.search(keyword.value, { page, size: 10 })) as unknown as {
        data: PageResult<ArticleDTO>
      }

      if (page === 0) {
        articles.value = res.data.content
      } else {
        articles.value = [...articles.value, ...res.data.content]
      }
      currentPage.value = res.data.pageNumber
      hasMore.value = !res.data.last
      total.value = res.data.totalElements
      if (page === 0) {
        if (res.data.totalElements === 0) {
          await loadSuggestions()
        } else {
          suggestions.value = []
        }
      }
    } catch (error) {
      console.error('Failed to search:', error)
    } finally {
      loading.value = false
    }
  }

  function loadMore() {
    if (hasMore.value && !loading.value) {
      search(currentPage.value + 1)
    }
  }

  watch(
    () => route.query.keyword,
    (newKeyword) => {
      keyword.value = (newKeyword as string) || ''
      suggestions.value = []
      if (keyword.value) {
        search(0)
      }
    },
    { immediate: true }
  )

  onMounted(() => {
    keyword.value = (route.query.keyword as string) || ''
    suggestions.value = []
    if (keyword.value) {
      search()
    }
  })
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        搜索: {{ keyword }}
      </h1>
      <p v-if="!loading" class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        找到 {{ total }} 篇相关文章
      </p>
    </div>

    <!-- Loading -->
    <div v-if="loading && articles.length === 0" class="flex justify-center py-12">
      <div class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- Empty -->
    <div v-else-if="articles.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
      未找到相关文章
      <div v-if="suggestions.length" class="mt-6 text-left max-w-2xl mx-auto">
        <p class="text-sm text-gray-500 dark:text-gray-400">猜你想看</p>
        <ul class="mt-3 space-y-2">
          <li v-for="suggestion in suggestions" :key="suggestion.id">
            <RouterLink
              :to="`/article/${suggestion.slug}`"
              class="text-gray-800 dark:text-gray-200 hover:text-blue-600 dark:hover:text-blue-400"
            >
              {{ suggestion.title }}
            </RouterLink>
          </li>
        </ul>
      </div>
      <p v-else-if="suggestionLoading" class="mt-6 text-sm text-gray-400">加载推荐中...</p>
    </div>

    <!-- Articles -->
    <div v-else>
      <ArticleCard v-for="article in articles" :key="article.id" :article="article" />

      <div v-if="hasMore" class="py-8 text-center">
        <button
          @click="loadMore"
          :disabled="loading"
          class="px-6 py-2 bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-700 disabled:opacity-50"
        >
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>
  </div>
</template>
