package com.example.demo.controller;

import com.example.demo.dto.talent.*;
import com.example.demo.entity.TalentCategory;
import com.example.demo.service.TalentPostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/talents")
public class TalentPostController {

    private final TalentPostService service;

    public TalentPostController(TalentPostService service) {
        this.service = service;
    }

    /**
     * SecurityContext 에서 현재 로그인한 사용자의 userId(String)를 꺼냄.
     * - 로그인 안 되어 있으면 null
     */
    private String currentUserIdOrNull() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null || a.getPrincipal() == null) ? null : (String) a.getPrincipal();
    }

    @PostMapping
    public ResponseEntity<TalentPostResponse> create(@Valid @RequestBody TalentPostCreateRequest req) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(service.create(userId, req));
    }

    // 예: ?page=0&size=20&sort=createdAt,desc&q=디자인&category=PHOTO&tagId=12
    @GetMapping
    public ResponseEntity<Page<TalentPostListItem>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TalentCategory category,
            @RequestParam(required = false, name = "author") String authorUserId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Sort s = Sort.by(sort.split(",")[0]);
        if (sort.endsWith(",desc")) s = s.descending();
        Pageable pageable = PageRequest.of(page, size, s);
        return ResponseEntity.ok(service.search(q, category, authorUserId, tagId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TalentPostResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAndIncreaseView(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TalentPostResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TalentPostUpdateRequest req
    ) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(service.update(id, userId, req));
    }

    /**
     * 게시글 삭제(soft delete)
     * - 작성자 본인만 가능
     * - 실제로는 status = DELETED 로만 바꿈
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            // GlobalExceptionHandler 가 IllegalArgumentException 잡아서 401/400 매핑하도록 되어 있다고 가정
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        service.softDelete(id, userId);
        // 프론트에서 res.ok 체크만 하니까 204로 깔끔하게 반환
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /* ========= 즐겨찾기 API ========= */

    /** 현재 로그인한 유저가 이 글을 즐겨찾기 했는지 조회 */
    @GetMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> isFavorite(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            // 비로그인 상태라면 항상 false
            return ResponseEntity.ok(Map.of("favorited", false));
        }
        boolean favorited = service.isFavorite(userId, id);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    /** 즐겨찾기 토글 (추가/삭제) */
    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        boolean favorited = service.toggleFavorite(userId, id);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    /** ⭐ 내 즐겨찾기 재능글 목록 */
    @GetMapping("/favorites")
    public ResponseEntity<Page<TalentPostListItem>> myFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort   // id 기준 정렬
    ) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        Sort s = Sort.by(sort.split(",")[0]);
        if (sort.endsWith(",desc")) s = s.descending();
        Pageable pageable = PageRequest.of(page, size, s);

        return ResponseEntity.ok(service.getMyFavorites(userId, pageable));
    }
}
