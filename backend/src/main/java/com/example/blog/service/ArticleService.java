package com.example.blog.service;

import com.example.blog.dto.request.ArticleRequest;
import com.example.blog.dto.response.ArticleDTO;
import com.example.blog.entity.Article;
import com.example.blog.entity.ArticleLike;
import com.example.blog.entity.Category;
import com.example.blog.entity.Tag;
import com.example.blog.entity.User;
import com.example.blog.exception.BusinessException;
import com.example.blog.cache.CacheNames;
import com.example.blog.repository.ArticleSpecifications;
import com.example.blog.repository.ArticleRepository;
import com.example.blog.repository.ArticleLikeRepository;
import com.example.blog.repository.CategoryRepository;
import com.example.blog.repository.TagRepository;
import com.example.blog.repository.UserRepository;
import com.example.blog.search.ArticleIndexEvent;
import com.example.blog.search.ArticleSearchService;
import com.example.blog.dto.response.ArticleSuggestionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;
    private final ArticleSearchService articleSearchService;

    @Cacheable(cacheNames = CacheNames.PUBLISHED_ARTICLES,
            key = "T(com.example.blog.cache.CacheKeys).pageKey(#pageable)",
            condition = "#pageable.pageNumber < @cacheProperties.maxCachedPages",
            sync = true)
    public Page<ArticleDTO> getPublishedArticles(Pageable pageable) {
        return articleRepository.findPublishedArticles(Article.Status.PUBLISHED, pageable)
                .map(ArticleDTO::fromEntityList);
    }

    public Page<ArticleDTO> getAllArticles(Pageable pageable) {
        return articleRepository.findAll(pageable).map(ArticleDTO::fromEntityList);
    }

    public Page<ArticleDTO> getArticlesByStatus(Article.Status status, Pageable pageable) {
        return articleRepository.findByStatus(status, pageable).map(ArticleDTO::fromEntityList);
    }

    public Page<ArticleDTO> getArticlesByCategory(Long categoryId, Pageable pageable) {
        if (!categoryService.existsByIdCached(categoryId)) {
            throw BusinessException.notFound("Category not found");
        }
        Category category = categoryRepository.getReferenceById(categoryId);
        return articleRepository.findByCategory(category, pageable).map(ArticleDTO::fromEntityList);
    }

    public Page<ArticleDTO> getArticlesByCategory(Long categoryId, Article.Status status, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("Category not found"));
        return articleRepository.findByCategoryAndStatus(category, status, pageable).map(ArticleDTO::fromEntityList);
    }

    public Page<ArticleDTO> getArticlesByTag(Long tagId, Pageable pageable) {
        if (!tagService.existsByIdCached(tagId)) {
            throw BusinessException.notFound("Tag not found");
        }
        Tag tag = tagRepository.getReferenceById(tagId);
        return articleRepository.findByTagsContaining(tag, pageable).map(ArticleDTO::fromEntityList);
    }

    @Cacheable(cacheNames = CacheNames.ARTICLES_BY_CATEGORY,
            key = "T(com.example.blog.cache.CacheKeys).pageKey(#categoryId, #pageable)",
            condition = "#pageable.pageNumber < @cacheProperties.maxCachedPages",
            sync = true)
    public Page<ArticleDTO> getPublishedArticlesByCategory(Long categoryId, Pageable pageable) {
        if (!categoryService.existsByIdCached(categoryId)) {
            throw BusinessException.notFound("Category not found");
        }
        Category category = categoryRepository.getReferenceById(categoryId);
        return articleRepository.findByCategoryAndStatus(category, Article.Status.PUBLISHED, pageable)
                .map(ArticleDTO::fromEntityList);
    }

    @Cacheable(cacheNames = CacheNames.ARTICLES_BY_TAG,
            key = "T(com.example.blog.cache.CacheKeys).pageKey(#tagId, #pageable)",
            condition = "#pageable.pageNumber < @cacheProperties.maxCachedPages",
            sync = true)
    public Page<ArticleDTO> getPublishedArticlesByTag(Long tagId, Pageable pageable) {
        if (!tagService.existsByIdCached(tagId)) {
            throw BusinessException.notFound("Tag not found");
        }
        Tag tag = tagRepository.getReferenceById(tagId);
        return articleRepository.findByTagsContainingAndStatus(tag, Article.Status.PUBLISHED, pageable)
                .map(ArticleDTO::fromEntityList);
    }

    @Cacheable(cacheNames = CacheNames.HOT_ARTICLES,
            key = "T(com.example.blog.cache.CacheKeys).sizeKey(#size)",
            condition = "#size <= @cacheProperties.maxHotSize",
            sync = true)
    public List<ArticleDTO> getHotArticles(int size) {
        return articleRepository.findHotPublishedArticles(PageRequest.of(0, size))
                .map(ArticleDTO::fromEntityList)
                .toList();
    }

    public Page<ArticleDTO> getArticlesByTag(Long tagId, Article.Status status, Pageable pageable) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> BusinessException.notFound("Tag not found"));
        return articleRepository.findByTagsContainingAndStatus(tag, status, pageable).map(ArticleDTO::fromEntityList);
    }

    public Page<ArticleDTO> searchArticles(String keyword, Pageable pageable) {
        return articleSearchService.search(keyword, pageable);
    }

    public Page<ArticleDTO> searchArticles(Article.Status status,
                                           Long categoryId,
                                           Long tagId,
                                           String keyword,
                                           Pageable pageable) {
        Specification<Article> specification = ArticleSpecifications.withFilters(status, categoryId, tagId, keyword);
        return articleRepository.findAll(specification, pageable).map(ArticleDTO::fromEntityList);
    }

    public List<ArticleSuggestionDTO> suggestArticles(String keyword, int size) {
        return articleSearchService.suggest(keyword, size);
    }

    @Transactional(readOnly = true)
    public ArticleDTO getArticleById(Long id) {
        return getArticleById(id, null);
    }

    @Transactional(readOnly = true)
    public ArticleDTO getArticleById(Long id, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));
        Boolean liked = resolveLiked(article, username);
        return ArticleDTO.fromEntity(article, liked);
    }

    @Transactional(readOnly = true)
    public ArticleDTO getArticleBySlug(String slug) {
        return getArticleBySlug(slug, null);
    }

    @Transactional(readOnly = true)
    public ArticleDTO getArticleBySlug(String slug, String username) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));
        Boolean liked = resolveLiked(article, username);
        return ArticleDTO.fromEntity(article, liked);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLISHED_ARTICLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_CATEGORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_TAG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.HOT_ARTICLES, allEntries = true)
    })
    public ArticleDTO createArticle(ArticleRequest request, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle()));
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setStatus(Article.Status.valueOf(request.getStatus()));
        article.setIsTop(request.getIsTop());
        article.setAllowComment(request.getAllowComment());
        article.setAuthor(author);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BusinessException.notFound("Category not found"));
            article.setCategory(category);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            article.setTags(tags);
        }

        if (article.getStatus() == Article.Status.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now());
        }

        articleRepository.save(article);
        eventPublisher.publishEvent(ArticleIndexEvent.upsert(article.getId()));
        return ArticleDTO.fromEntity(article);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLISHED_ARTICLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_CATEGORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_TAG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.HOT_ARTICLES, allEntries = true)
    })
    public ArticleDTO updateArticle(Long id, ArticleRequest request, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw BusinessException.forbidden("You don't have permission to edit this article");
        }

        article.setTitle(request.getTitle());
        article.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle()));
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setIsTop(request.getIsTop());
        article.setAllowComment(request.getAllowComment());

        Article.Status newStatus = Article.Status.valueOf(request.getStatus());
        if (article.getStatus() != Article.Status.PUBLISHED && newStatus == Article.Status.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now());
        }
        article.setStatus(newStatus);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> BusinessException.notFound("Category not found"));
            article.setCategory(category);
        } else {
            article.setCategory(null);
        }

        if (request.getTagIds() != null) {
            Set<Tag> tags = tagRepository.findByIdIn(request.getTagIds());
            article.setTags(tags);
        } else {
            article.setTags(new HashSet<>());
        }

        articleRepository.save(article);
        eventPublisher.publishEvent(ArticleIndexEvent.upsert(article.getId()));
        return ArticleDTO.fromEntity(article);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PUBLISHED_ARTICLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_CATEGORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_TAG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.HOT_ARTICLES, allEntries = true)
    })
    public void deleteArticle(Long id, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw BusinessException.forbidden("You don't have permission to delete this article");
        }

        articleRepository.delete(article);
        eventPublisher.publishEvent(ArticleIndexEvent.delete(article.getId()));
    }

    @Transactional
    public void incrementViewCount(Long id) {
        articleRepository.incrementViewCount(id);
    }

    @Transactional
    public LikeResult toggleLike(Long id, String username) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Article not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        Optional<ArticleLike> existing = articleLikeRepository.findByArticleAndUser(article, user);
        boolean liked;
        if (existing.isPresent()) {
            articleLikeRepository.delete(existing.get());
            article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
            liked = false;
        } else {
            ArticleLike articleLike = new ArticleLike();
            articleLike.setArticle(article);
            articleLike.setUser(user);
            articleLikeRepository.save(articleLike);
            article.setLikeCount(article.getLikeCount() + 1);
            liked = true;
        }
        articleRepository.save(article);
        return new LikeResult(article.getLikeCount(), liked);
    }

    private Boolean resolveLiked(Article article, String username) {
        if (username == null) {
            return null;
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return null;
        }
        return articleLikeRepository.existsByArticleAndUser(article, user);
    }

    private String generateSlug(String title) {
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        // 如果标题是纯中文或特殊字符，slug会为空，此时使用时间戳
        if (slug.isEmpty()) {
            slug = "article-" + System.currentTimeMillis();
        }
        return slug;
    }

    public record LikeResult(int likeCount, boolean liked) {}
}
