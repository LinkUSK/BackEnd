// src/main/java/com/example/demo/controller/MeController.java
package com.example.demo.controller;

import com.example.demo.dto.MeResponse;
import com.example.demo.dto.UpdateMeRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.TalentPostRepository;
import com.example.demo.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;
    private final TalentPostRepository talentPostRepository;

    public MeController(UserRepository userRepository,
                        TalentPostRepository talentPostRepository) {
        this.userRepository = userRepository;
        this.talentPostRepository = talentPostRepository;
    }

    private User currentUserOrThrow() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        String userId = (String) auth.getPrincipal();
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private String formatCreatedAt(User u) {
        if (u.getCreatedAt() == null) return null;
        return u.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /** 내 정보 조회 */
    @GetMapping
    public ResponseEntity<MeResponse> me() {
        User u = currentUserOrThrow();
        String createdAt = formatCreatedAt(u);   // ✅

        return ResponseEntity.ok(
                new MeResponse(
                        u.getId(),
                        u.getUsername(),
                        u.getUserId(),
                        u.getEmail(),
                        u.getMajor(),
                        u.getProfileImageUrl(),
                        createdAt
                )
        );
    }

    /** 내 정보 수정 (이름/전공/프로필 이미지) */
    @PatchMapping
    public ResponseEntity<MeResponse> update(@Valid @RequestBody UpdateMeRequest req) {
        User u = currentUserOrThrow();

        if (req.username() != null) u.setUsername(req.username().trim());
        if (req.major() != null) u.setMajor(req.major().trim());
        if (req.profileImageUrl() != null) u.setProfileImageUrl(req.profileImageUrl().trim());

        User saved = userRepository.save(u);
        String createdAt = formatCreatedAt(saved);   // ✅

        return ResponseEntity.ok(
                new MeResponse(
                        saved.getId(),
                        saved.getUsername(),
                        saved.getUserId(),
                        saved.getEmail(),
                        saved.getMajor(),
                        saved.getProfileImageUrl(),
                        createdAt
                )
        );
    }

    /** 회원 탈퇴 (게시글 포함 삭제) */
    @DeleteMapping
    @Transactional
    public ResponseEntity<?> delete() {
        User u = currentUserOrThrow();
        talentPostRepository.deleteAllByAuthor(u);
        userRepository.delete(u);
        return ResponseEntity.ok(java.util.Map.of("message", "deleted"));
    }
}
