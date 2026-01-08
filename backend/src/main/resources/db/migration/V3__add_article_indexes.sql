-- V3__add_article_indexes.sql
-- Indexes for hot/list queries and tag lookups

CREATE INDEX IF NOT EXISTS idx_articles_status_is_top_published_at
    ON articles(status, is_top, published_at);

CREATE INDEX IF NOT EXISTS idx_articles_status_view_count
    ON articles(status, view_count, published_at);

CREATE INDEX IF NOT EXISTS idx_articles_category_status_published_at
    ON articles(category_id, status, published_at);

CREATE INDEX IF NOT EXISTS idx_article_tags_tag_id
    ON article_tags(tag_id);
