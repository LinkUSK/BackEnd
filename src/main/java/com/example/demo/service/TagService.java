// com.example.demo.service.TagService.java (delete 메서드 추가)
package com.example.demo.service;

import com.example.demo.dto.meta.TagCreateRequest;
import com.example.demo.dto.meta.TagResponse;
import com.example.demo.entity.Tag;
import com.example.demo.entity.TalentCategory;
import com.example.demo.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<String> getCategories() {
        return List.of("PHOTO", "MODEL", "PROGRAMMING", "DESIGN", "MUSIC");
    }

    public List<TagResponse> getTagsByCategory(String category) {
        TalentCategory cat = toCat(category);
        return tagRepository.findByCategoryAndActiveTrueOrderByNameAsc(cat).stream()
                .map(t -> new TagResponse(t.getId(), t.getCategory().name(), t.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public TagResponse createOrReviveTag(TagCreateRequest req) {
        TalentCategory cat = toCat(req.category());
        String name = req.name().trim();

        var maybe = tagRepository.findByCategoryAndNameIgnoreCase(cat, name);
        if (maybe.isPresent()) {
            Tag t = maybe.get();
            if (!t.isActive()) {
                t.setActive(true);
                tagRepository.save(t);
            }
            return new TagResponse(t.getId(), t.getCategory().name(), t.getName());
        }
        Tag saved = tagRepository.save(Tag.builder()
                .category(cat).name(name).active(true).build());
        return new TagResponse(saved.getId(), saved.getCategory().name(), saved.getName());
    }

    @Transactional
    public void deactivateTagIfExists(Long id) {
        tagRepository.findById(id).ifPresent(t -> {
            if (t.isActive()) {
                t.setActive(false);
                tagRepository.save(t);
            }
        }); // 존재하지 않으면 조용히 무시 ⇒ idempotent
    }

    private TalentCategory toCat(String category) {
        try { return TalentCategory.valueOf(category.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("잘못된 카테고리: " + category); }
    }
}