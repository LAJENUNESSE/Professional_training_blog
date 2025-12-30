export type ArticleStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface AuthorInfo {
  id: number
  username: string
  nickname: string | null
  avatar: string | null
}

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
  category: import('./category').CategoryDTO | null
  tags: import('./tag').TagDTO[]
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

export interface ArticleLikeResponse {
  likeCount: number
  liked: boolean
}
