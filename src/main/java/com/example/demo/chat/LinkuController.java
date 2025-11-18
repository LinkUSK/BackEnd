// src/main/java/com/example/demo/chat/LinkuController.java
package com.example.demo.chat;

import com.example.demo.chat.dto.LinkuMyConnectionRes;
import com.example.demo.chat.dto.LinkuProposeReq;
import com.example.demo.chat.dto.LinkuRatingSummaryRes;
import com.example.demo.chat.dto.LinkuReviewReq;
import com.example.demo.chat.dto.LinkuReviewRes;
import com.example.demo.chat.dto.LinkuStateRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class LinkuController {

    private final LinkuService linkuService;

    private String currentUserIdOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return null;
        return (String) a.getPrincipal();
    }

    // ===== LinkU 상태 조회 =====
    @GetMapping("/rooms/{roomId}/linku")
    public ResponseEntity<LinkuStateRes> getState(@PathVariable Long roomId) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(linkuService.getState(roomId, userId));
    }

    // ===== LinkU 제안 =====
    @PostMapping("/rooms/{roomId}/linku/propose")
    public ResponseEntity<LinkuStateRes> propose(
            @PathVariable Long roomId,
            @RequestBody LinkuProposeReq req
    ) {
        String userId = currentUserIdOrNull();
        if (userId == null) return ResponseEntity.status(401).build();

        LinkuStateRes res = linkuService.propose(
                roomId,
                userId,
                req.getTargetUserId(),
                req.getMessage(),
                req.getTalentPostId()   // 🔹 추가
        );
        return ResponseEntity.ok(res);
    }

    // ===== LinkU 수락 =====
    @PostMapping("/linku/{id}/accept")
    public ResponseEntity<LinkuStateRes> accept(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) return ResponseEntity.status(401).build();

        LinkuStateRes res = linkuService.accept(id, userId);
        return ResponseEntity.ok(res);
    }

    // ===== LinkU 거절 =====
    @PostMapping("/linku/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) return ResponseEntity.status(401).build();

        linkuService.reject(id, userId);
        return ResponseEntity.ok().build();
    }

    // ===== 후기 작성 =====
    @PostMapping("/rooms/{roomId}/linku/reviews")
    public ResponseEntity<Void> writeReview(
            @PathVariable Long roomId,
            @RequestBody LinkuReviewReq req
    ) {
        String userId = currentUserIdOrNull();
        if (userId == null) return ResponseEntity.status(401).build();

        linkuService.writeReview(roomId, userId, req);
        return ResponseEntity.ok().build();
    }

    // ===== 내가 받은 LinkU 후기 목록 조회 =====
    @GetMapping("/linku/reviews/me")
    public ResponseEntity<List<LinkuReviewRes>> myReviews() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<LinkuReviewRes> res = linkuService.getMyReviews(userId);
        return ResponseEntity.ok(res);
    }

    // ===== 특정 유저(userId)가 받은 LinkU 후기 목록 조회 (프로필용) =====
    @GetMapping("/linku/reviews/user-id/{userId}")
    public ResponseEntity<List<LinkuReviewRes>> reviewsByUserId(@PathVariable String userId) {
        String me = currentUserIdOrNull();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        List<LinkuReviewRes> res = linkuService.getUserReviewsByLoginId(userId);
        return ResponseEntity.ok(res);
    }

    // ===== 후기 삭제 (내가 보낸/받은 모두 여기로) =====
    @DeleteMapping("/linku/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        linkuService.deleteReview(id, userId);
        return ResponseEntity.ok().build();
    }

    // ===== ⭐ 내 별점 평균 / 리뷰 개수 =====
    @GetMapping("/linku/rating/me")
    public ResponseEntity<LinkuRatingSummaryRes> myRatingSummary() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        LinkuRatingSummaryRes res = linkuService.getMyRatingSummary(userId);
        return ResponseEntity.ok(res);
    }

    // ===== ⭐ 특정 유저(타인) 별점 (로그인 아이디 기준) =====
    @GetMapping("/linku/rating/user-id/{userId}")
    public ResponseEntity<LinkuRatingSummaryRes> userRatingSummaryByLoginId(
            @PathVariable String userId
    ) {
        String me = currentUserIdOrNull();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        LinkuRatingSummaryRes res = linkuService.getUserRatingSummaryByLoginId(userId);
        return ResponseEntity.ok(res);
    }

    // (선택) PK 기준 버전
    @GetMapping("/linku/rating/{userPk}")
    public ResponseEntity<LinkuRatingSummaryRes> userRatingSummaryByPk(
            @PathVariable Long userPk
    ) {
        String me = currentUserIdOrNull();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        LinkuRatingSummaryRes res = linkuService.getUserRatingSummary(userPk);
        return ResponseEntity.ok(res);
    }

    // ===== ⭐ 내 링크유 목록 =====
    @GetMapping("/linku/connections/me")
    public ResponseEntity<List<LinkuMyConnectionRes>> myConnections() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<LinkuMyConnectionRes> res = linkuService.getMyConnections(userId);
        return ResponseEntity.ok(res);
    }
}
