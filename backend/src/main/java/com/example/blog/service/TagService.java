package com.example.blog.service;

import com.example.blog.dto.request.TagRequest;
import com.example.blog.dto.response.TagDTO;
import com.example.blog.entity.Tag;
import com.example.blog.exception.BusinessException;
import com.example.blog.cache.CacheNames;
import com.example.blog.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Cacheable(cacheNames = CacheNames.TAG_LIST, sync = true)
    public List<TagDTO> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(TagDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = CacheNames.TAG_EXISTS, key = "#id", sync = true)
    public boolean existsByIdCached(Long id) {
        return tagRepository.existsById(id);
    }

    public Page<TagDTO> getAllTagsPaged(Pageable pageable) {
        return tagRepository.findAll(pageable).map(TagDTO::fromEntity);
    }

    public TagDTO getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tag not found"));
        return TagDTO.fromEntity(tag);
    }

    public TagDTO getTagBySlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Tag not found"));
        return TagDTO.fromEntity(tag);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TAG_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.TAG_EXISTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLISHED_ARTICLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_CATEGORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_TAG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.HOT_ARTICLES, allEntries = true)
    })
    public TagDTO createTag(TagRequest request) {
        if (tagRepository.existsByName(request.getName())) {
            throw BusinessException.badRequest("Tag name already exists");
        }

        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getName()));

        tagRepository.save(tag);
        return TagDTO.fromEntitySimple(tag);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TAG_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.TAG_EXISTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLISHED_ARTICLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_CATEGORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_TAG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.HOT_ARTICLES, allEntries = true)
    })
    public TagDTO updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tag not found"));

        if (!tag.getName().equals(request.getName()) && tagRepository.existsByName(request.getName())) {
            throw BusinessException.badRequest("Tag name already exists");
        }

        tag.setName(request.getName());
        tag.setSlug(request.getSlug() != null ? request.getSlug() : generateSlug(request.getName()));

        tagRepository.save(tag);
        return TagDTO.fromEntitySimple(tag);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.TAG_LIST, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.TAG_EXISTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PUBLISHED_ARTICLES, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_CATEGORY, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.ARTICLES_BY_TAG, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.HOT_ARTICLES, allEntries = true)
    })
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Tag not found"));

        if (!tag.getArticles().isEmpty()) {
            throw BusinessException.badRequest("Cannot delete tag with articles");
        }

        tagRepository.delete(tag);
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
