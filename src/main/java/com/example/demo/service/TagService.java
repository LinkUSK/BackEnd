// com.example.demo.service.TagService.java
package com.example.demo.service;

import com.example.demo.dto.meta.TagCreateRequest;
import com.example.demo.dto.meta.TagResponse;
import com.example.demo.entity.Tag;
import com.example.demo.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * 🔥 현재 활성 태그들이 사용 중인 카테고리 목록을 모두 반환
     *  - 직접 입력한 카테고리도 여기 포함됨
     */
    public List<String> getCategories() {
        return tagRepository.findDistinctActiveCategories();
    }

    /**
     * 🔥 카테고리 문자열 그대로 사용
     */
    public List<TagResponse> getTagsByCategory(String category) {
        String cat = normalizeCategory(category);
        return tagRepository.findByCategoryAndActiveTrueOrderByNameAsc(cat).stream()
                .map(t -> new TagResponse(t.getId(), t.getCategory(), t.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public TagResponse createOrReviveTag(TagCreateRequest req) {
        String cat = normalizeCategory(req.category());
        String name = normalizeName(req.name());

        var maybe = tagRepository.findByCategoryAndNameIgnoreCase(cat, name);
        if (maybe.isPresent()) {
            Tag t = maybe.get();
            if (!t.isActive()) {
                t.setActive(true);
                tagRepository.save(t);
            }
            return new TagResponse(t.getId(), t.getCategory(), t.getName());
        }

        Tag saved = tagRepository.save(
                Tag.builder()
                        .category(cat)
                        .name(name)
                        .active(true)
                        .build()
        );
        return new TagResponse(saved.getId(), saved.getCategory(), saved.getName());
    }

    @Transactional
    public void deactivateTagIfExists(Long id) {
        tagRepository.findById(id).ifPresent(t -> {
            if (t.isActive()) {
                t.setActive(false);
                tagRepository.save(t);
            }
        });
    }

    /* ===== 작은 유틸들 ===== */

    private String normalizeCategory(String c) {
        if (c == null) return "기타";
        String t = c.trim();
        return t.isEmpty() ? "기타" : t;
    }

    private String normalizeName(String n) {
        if (n == null) {
            throw new IllegalArgumentException("태그 이름은 비어 있을 수 없습니다.");
        }
        String t = n.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("태그 이름은 비어 있을 수 없습니다.");
        }
        return t;
    }
}
