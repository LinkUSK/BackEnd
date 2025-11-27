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

/**
 * 🔹 LinkU 협업 흐름 API
 *  - 채팅방에서 LinkU 제안/수락/거절
 *  - 협업 완료 후 후기 작성
 *  - 유저별 별점 요약/후기 목록
 *  - 내 LinkU 연결 목록 조회
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class LinkuController {

    private final LinkuService linkuService;

    /** SecurityContext 에서 현재 로그인한 userId(문자열) 조회 */
    private String currentUserIdOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return null;
        return (String) a.getPrincipal();
    }

    // ===== LinkU 상태 조회 =====

    /**
     * 📌 특정 채팅방에서 내 기준 LinkU 상태 조회
     * - 아직 제안 전인지, 대기 중인지, 수락/거절/완료인지 한 번에 반환
     */
    @GetMapping("/rooms/{roomId}/linku")
    public ResponseEntity<LinkuStateRes> getState(@PathVariable Long roomId) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(linkuService.getState(roomId, userId));
    }

    // ===== LinkU 제안 =====

    /**
     * 🤝 LinkU 제안
     * - 채팅방(roomId) 안에서 상대에게 협업을 제안
     * - 대상 유저, 간단한 메세지, 연결된 재능글 ID 를 함께 전달
     */
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
                req.getTalentPostId()   // 🔹 어떤 재능글에서 시작된 협업인지 연결
        );
        return ResponseEntity.ok(res);
    }

    // ===== LinkU 수락 / 거절 =====

    /** ✅ LinkU 수락 */
    @PostMapping("/linku/{id}/accept")
    public ResponseEntity<LinkuStateRes> accept(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) return ResponseEntity.status(401).build();

        LinkuStateRes res = linkuService.accept(id, userId);
        return ResponseEntity.ok(res);
    }

    /** ❌ LinkU 거절 */
    @PostMapping("/linku/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) return ResponseEntity.status(401).build();

        linkuService.reject(id, userId);
        return ResponseEntity.ok().build();
    }

    // ===== 후기 작성 / 조회 / 삭제 =====

    /**
     * 📝 LinkU 후기 작성
     * - 채팅방 기준으로 누구와 협업했는지 파악
     * - 점수 + 코멘트를 기록
     */
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

    /** 📄 내가 받은 LinkU 후기 목록 */
    @GetMapping("/linku/reviews/me")
    public ResponseEntity<List<LinkuReviewRes>> myReviews() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<LinkuReviewRes> res = linkuService.getMyReviews(userId);
        return ResponseEntity.ok(res);
    }

    /** 📄 특정 유저가 받은 후기 목록 (프로필 화면용) */
    @GetMapping("/linku/reviews/user-id/{userId}")
    public ResponseEntity<List<LinkuReviewRes>> reviewsByUserId(@PathVariable String userId) {
        String me = currentUserIdOrNull();
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        List<LinkuReviewRes> res = linkuService.getUserReviewsByLoginId(userId);
        return ResponseEntity.ok(res);
    }

    /** 🗑 후기 삭제 (내가 남긴/받은 것 중 하나) */
    @DeleteMapping("/linku/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        linkuService.deleteReview(id, userId);
        return ResponseEntity.ok().build();
    }

    // ===== 별점 요약 =====

    /** ⭐ 내가 받은 별점 평균 + 리뷰 개수 */
    @GetMapping("/linku/rating/me")
    public ResponseEntity<LinkuRatingSummaryRes> myRatingSummary() {
        String userId = currentUserIdOrNull();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        LinkuRatingSummaryRes res = linkuService.getMyRatingSummary(userId);
        return ResponseEntity.ok(res);
    }

    /** ⭐ 특정 유저(userId)의 별점 요약 (프로필용) */
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

    /** (선택) PK 기준 버전 */
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

    // ===== 내 LinkU 목록 =====

    /**
     * 🤝 내가 참여한 LinkU 목록
     * - 마이페이지 > LinkU 탭에서 사용
     * - 상대 프로필, 상태, 연결된 재능글 정보 등을 한 번에 내려줌
     */
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
