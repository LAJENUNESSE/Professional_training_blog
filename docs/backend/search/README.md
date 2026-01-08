# 搜索集成 (Meilisearch)

## 概述

后端已集成 Meilisearch，用于替代数据库 `LIKE` 查询，支持全文检索、相关性排序、自动拼写纠错与高亮返回。
当搜索服务不可用或未启用时，自动回退到数据库查询。

## 启用配置

在 `backend/src/main/resources/application.yml` 中设置：

```yaml
blog:
  search:
    enabled: true
    engine: MEILISEARCH
    reindex-batch-size: 200
    meilisearch:
      host: http://localhost:7700
      api-key:
      index: articles
```

## 启动 Meilisearch（2c2g 低配建议）

示例（本地二进制）：

```bash
MEILI_NO_ANALYTICS=true \
MEILI_MAX_INDEXING_MEMORY=256MB \
MEILI_MAX_INDEXING_THREADS=2 \
./meilisearch --http-addr 0.0.0.0:7700
```

## 主要接口

- 搜索：`GET /api/articles/search?keyword=xxx&page=0&size=10`
- 搜索建议：`GET /api/articles/suggest?keyword=xxx&size=5`
- 全量重建索引：`POST /api/admin/search/reindex`

## 高亮字段

搜索结果会附加以下字段（已做 HTML 安全处理，仅保留 `<mark>`）：

- `highlightTitle`
- `highlightSummary`
- `highlightContent`

前端可用 `v-html` 渲染这些字段以展示高亮效果。
