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
  } from '@blog/shared'
  import { formatDate } from '@blog/shared'
  import 'highlight.js/styles/github-dark.css'

  const route = useRoute()
  const authStore = useAuthStore()
  const article = ref<ArticleDTO | null>(null)
  const comments = ref<CommentDTO[]>([])
  const loading = ref(true)
  const error = ref<string | null>(null)
  const liked = ref<boolean>(false)
  const likeLoading = ref(false)
  const commentForm = reactive<CommentRequest>({
    content: '',
    authorName: '',
    authorEmail: '',
    authorUrl: '',
  })
  const submittingComment = ref(false)
  const commentFeedback = ref<string | null>(null)
  const commentError = ref<string | null>(null)

  const md = new MarkdownIt({
    html: true,
    linkify: true,
    highlight: (str: string, lang: string) => {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(str, { language: lang }).value
        } catch (_) {
          /* empty */
        }
      }
      return ''
    },
  })

  const renderedContent = computed(() => {
    if (!article.value) return ''
    return md.render(article.value.content)
  })

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

      // Load comments
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

  watch(() => route.params.slug, loadArticle)

  onMounted(loadArticle)
</script>

<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div
        class="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"
      ></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="text-center py-12 text-red-500">
      {{ error }}
    </div>

    <!-- Article Content -->
    <article v-else-if="article" class="pb-12">
      <!-- Header -->
      <header class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-4">
          {{ article.title }}
        </h1>

        <div class="flex items-center flex-wrap gap-4 text-sm text-gray-500 dark:text-gray-400">
          <span>{{ article.author.nickname || article.author.username }}</span>
          <span>{{ formatDate(article.publishedAt || article.createdAt, 'YYYY年MM月DD日') }}</span>
          <span v-if="article.category">
            <RouterLink
              :to="`/category/${article.category.id}`"
              class="hover:text-blue-600 dark:hover:text-blue-400"
            >
              {{ article.category.name }}
            </RouterLink>
          </span>
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
        <div v-if="article.tags.length" class="mt-4 flex flex-wrap gap-2">
          <RouterLink
            v-for="tag in article.tags"
            :key="tag.id"
            :to="`/tag/${tag.id}`"
            class="px-2 py-0.5 text-sm bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
          >
            #{{ tag.name }}
          </RouterLink>
        </div>
      </header>

      <!-- Cover Image -->
      <div v-if="article.coverImage" class="mb-8">
        <img
          :src="article.coverImage"
          :alt="article.title"
          class="w-full rounded-lg shadow-md"
        />
      </div>

      <!-- Content -->
      <div
        class="prose prose-lg dark:prose-invert max-w-none"
        v-html="renderedContent"
      ></div>

      <!-- Actions -->
      <div class="mt-8 pt-8 border-t border-gray-200 dark:border-gray-700">
        <button
          @click="handleLike"
          :disabled="likeLoading"
          class="inline-flex items-center gap-2 px-4 py-2 rounded-lg transition disabled:opacity-60"
          :class="liked ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-200' : 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-700'"
        >
          <svg
            class="w-5 h-5"
            :fill="liked ? 'currentColor' : 'none'"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
            />
          </svg>
          {{ article.likeCount }}
        </button>
      </div>

      <!-- Comments Section -->
      <section v-if="article.allowComment" class="mt-12">
        <h2 class="text-xl font-semibold text-gray-900 dark:text-white mb-6">
          评论 ({{ comments.length }})
        </h2>

        <div class="mb-6 space-y-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-2">
              发表你的看法
            </label>
            <textarea
              v-model="commentForm.content"
              rows="4"
              class="w-full rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="写下你的观点..."
            ></textarea>
          </div>
          <div v-if="!authStore.isAuthenticated" class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <input
              v-model="commentForm.authorName"
              type="text"
              class="w-full rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="昵称（必填）"
            />
            <input
              v-model="commentForm.authorEmail"
              type="email"
              class="w-full rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="邮箱（选填，用于头像或通知）"
            />
            <input
              v-model="commentForm.authorUrl"
              type="url"
              class="w-full rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 md:col-span-2"
              placeholder="个人站点（选填）"
            />
          </div>
          <div class="flex items-center gap-3">
            <button
              class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-60"
              :disabled="submittingComment"
              @click="handleSubmitComment"
            >
              {{ submittingComment ? '提交中...' : '发表评论' }}
            </button>
            <p v-if="commentFeedback" class="text-sm text-green-600 dark:text-green-400">
              {{ commentFeedback }}
            </p>
            <p v-if="commentError" class="text-sm text-red-500">
              {{ commentError }}
            </p>
          </div>
        </div>

        <div v-if="comments.length === 0" class="text-gray-500 dark:text-gray-400">
          暂无评论
        </div>

        <div v-else class="space-y-6">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg"
          >
            <div class="flex items-center gap-2 mb-2">
              <span class="font-medium text-gray-900 dark:text-white">
                {{ comment.authorName || '匿名用户' }}
              </span>
              <span class="text-sm text-gray-500 dark:text-gray-400">
                {{ formatDate(comment.createdAt, 'YYYY-MM-DD HH:mm') }}
              </span>
            </div>
            <p class="text-gray-700 dark:text-gray-300">{{ comment.content }}</p>
          </div>
        </div>
      </section>
    </article>
  </div>
</template>
