<script setup lang="ts">
  import { computed } from 'vue'
  import { RouterLink } from 'vue-router'
  import type { ArticleDTO } from '@blog/shared'
  import { formatRelativeTime } from '@blog/shared'

  const props = defineProps<{
    article: ArticleDTO
  }>()

  const highlightTitle = computed(() => props.article.highlightTitle || '')
  const highlightSnippet = computed(
    () => props.article.highlightSummary || props.article.highlightContent || ''
  )
</script>

<template>
  <article class="py-6 border-b border-gray-100 dark:border-gray-800 last:border-0">
    <RouterLink :to="`/article/${article.slug}`" class="block group">
      <!-- Cover Image -->
      <div v-if="article.coverImage" class="mb-4 overflow-hidden rounded-lg">
        <img
          :src="article.coverImage"
          :alt="article.title"
          class="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
        />
      </div>

      <!-- Content -->
      <div>
        <!-- Top Badge -->
        <span
          v-if="article.isTop"
          class="inline-block px-2 py-0.5 text-xs font-medium text-red-600 bg-red-50 dark:bg-red-900/20 dark:text-red-400 rounded mb-2"
        >
          置顶
        </span>

        <!-- Title -->
        <h2
          class="text-xl font-semibold text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 mb-2"
        >
          <span v-if="highlightTitle" v-html="highlightTitle"></span>
          <span v-else>{{ article.title }}</span>
        </h2>

        <!-- Summary -->
        <p
          v-if="highlightSnippet"
          class="text-gray-600 dark:text-gray-400 line-clamp-2 mb-3"
          v-html="highlightSnippet"
        ></p>
        <p v-else-if="article.summary" class="text-gray-600 dark:text-gray-400 line-clamp-2 mb-3">
          {{ article.summary }}
        </p>

        <!-- Meta -->
        <div class="flex items-center flex-wrap gap-3 text-sm text-gray-500 dark:text-gray-400">
          <span>{{ article.author.nickname || article.author.username }}</span>
          <span v-if="article.category">
            <RouterLink
              :to="`/category/${article.category.id}`"
              class="hover:text-blue-600 dark:hover:text-blue-400"
              @click.stop
            >
              {{ article.category.name }}
            </RouterLink>
          </span>
          <span>{{ formatRelativeTime(article.publishedAt || article.createdAt) }}</span>
          <span class="flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
              />
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
              />
            </svg>
            {{ article.viewCount }}
          </span>
        </div>

        <!-- Tags -->
        <div v-if="article.tags.length" class="mt-3 flex flex-wrap gap-2">
          <RouterLink
            v-for="tag in article.tags"
            :key="tag.id"
            :to="`/tag/${tag.id}`"
            class="px-2 py-0.5 text-xs bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
            @click.stop
          >
            #{{ tag.name }}
          </RouterLink>
        </div>
      </div>
    </RouterLink>
  </article>
</template>
