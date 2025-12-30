import request from './request'
import type {
  Result,
  PageResult,
  PageParams,
  ArticleDTO,
  ArticleRequest,
  ArticleLikeResponse,
} from '../types'

export const articleApi = {
  // 公开接口
  getPublished: (params?: PageParams) =>
    request.get<Result<PageResult<ArticleDTO>>>('/api/articles', { params }),

  getById: (id: number) => request.get<Result<ArticleDTO>>(`/api/articles/${id}`),

  getBySlug: (slug: string) => request.get<Result<ArticleDTO>>(`/api/articles/slug/${slug}`),

  getByCategory: (categoryId: number, params?: PageParams) =>
    request.get<Result<PageResult<ArticleDTO>>>(`/api/articles/category/${categoryId}`, { params }),

  getByTag: (tagId: number, params?: PageParams) =>
    request.get<Result<PageResult<ArticleDTO>>>(`/api/articles/tag/${tagId}`, { params }),

  search: (keyword: string, params?: PageParams) =>
    request.get<Result<PageResult<ArticleDTO>>>('/api/articles/search', {
      params: { keyword, ...params },
    }),

  like: (id: number) => request.post<Result<ArticleLikeResponse>>(`/api/articles/${id}/like`),

  // 管理接口
  admin: {
    getAll: (params?: PageParams & { status?: string; categoryId?: number; tagId?: number }) =>
      request.get<Result<PageResult<ArticleDTO>>>('/api/admin/articles', { params }),

    getById: (id: number) => request.get<Result<ArticleDTO>>(`/api/admin/articles/${id}`),

    create: (data: ArticleRequest) =>
      request.post<Result<ArticleDTO>>('/api/admin/articles', data),

    update: (id: number, data: ArticleRequest) =>
      request.put<Result<ArticleDTO>>(`/api/admin/articles/${id}`, data),

    delete: (id: number) => request.delete<Result<void>>(`/api/admin/articles/${id}`),
  },
}
