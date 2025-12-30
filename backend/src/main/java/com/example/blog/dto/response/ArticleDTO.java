package com.example.blog.dto.response;

import com.example.blog.entity.Article;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class ArticleDTO {

    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String coverImage;
    private String status;
    private Boolean isTop;
    private Boolean allowComment;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime publishedAt;
    private AuthorInfo author;
    private CategoryDTO category;
    private Set<TagDTO> tags;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class AuthorInfo {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
    }

    public static ArticleDTO fromEntity(Article article) {
        ArticleDTOBuilder builder = ArticleDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .content(article.getContent())
                .coverImage(article.getCoverImage())
                .status(article.getStatus().name())
                .isTop(article.getIsTop())
                .allowComment(article.getAllowComment())
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .liked(null)
                .publishedAt(article.getPublishedAt())
                .commentCount((long) article.getComments().size())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt());

        if (article.getAuthor() != null) {
            builder.author(AuthorInfo.builder()
                    .id(article.getAuthor().getId())
                    .username(article.getAuthor().getUsername())
                    .nickname(article.getAuthor().getNickname())
                    .avatar(article.getAuthor().getAvatar())
                    .build());
        }

        if (article.getCategory() != null) {
            builder.category(CategoryDTO.fromEntitySimple(article.getCategory()));
        }

        if (article.getTags() != null) {
            builder.tags(article.getTags().stream()
                    .map(TagDTO::fromEntitySimple)
                    .collect(Collectors.toSet()));
        }

        return builder.build();
    }

    public static ArticleDTO fromEntity(Article article, Boolean liked) {
        ArticleDTOBuilder builder = ArticleDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .summary(article.getSummary())
                .content(article.getContent())
                .coverImage(article.getCoverImage())
                .status(article.getStatus().name())
                .isTop(article.getIsTop())
                .allowComment(article.getAllowComment())
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .liked(liked)
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt());

        if (article.getAuthor() != null) {
            builder.author(AuthorInfo.builder()
                    .id(article.getAuthor().getId())
                    .username(article.getAuthor().getUsername())
                    .nickname(article.getAuthor().getNickname())
                    .avatar(article.getAuthor().getAvatar())
                    .build());
        }

        if (article.getCategory() != null) {
            builder.category(CategoryDTO.fromEntitySimple(article.getCategory()));
        }

        if (article.getTags() != null) {
            builder.tags(article.getTags().stream()
                    .map(TagDTO::fromEntitySimple)
                    .collect(Collectors.toSet()));
        }

        return builder.build();
    }

    public static ArticleDTO fromEntityList(Article article) {
        return fromEntity(article, null);
    }
}
