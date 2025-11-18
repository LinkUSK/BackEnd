// src/main/java/com/example/demo/chat/LinkuService.java
package com.example.demo.chat;

import com.example.demo.chat.LinkuConnection.LinkuStatus;
import com.example.demo.chat.LinkuReview.RelationRating;
import com.example.demo.chat.dto.LinkuMyConnectionRes;
import com.example.demo.chat.dto.LinkuRatingSummaryRes;
import com.example.demo.chat.dto.LinkuReviewReq;
import com.example.demo.chat.dto.LinkuReviewRes;
import com.example.demo.chat.dto.LinkuStateRes;
import com.example.demo.entity.User;
import com.example.demo.repository.TalentPostRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkuService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final LinkuConnectionRepository connectionRepository;
    private final LinkuReviewRepository reviewRepository;
    private final ChatService chatService;
    private final TalentPostRepository talentPostRepository;

    // ===== LinkU 상태 조회 =====
    @Transactional(readOnly = true)
    public LinkuStateRes getState(Long roomId, String currentUserLoginId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("room not found"));

        User current = findUserByUserId(currentUserLoginId);

        // 1순위: ACCEPTED 중 가장 최신
        LinkuConnection latest = connectionRepository
                .findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(room.getId(), LinkuStatus.ACCEPTED)
                .orElse(null);

        // 2순위: ACCEPTED 가 하나도 없으면 PENDING 중 가장 최신
        if (latest == null) {
            latest = connectionRepository
                    .findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(room.getId(), LinkuStatus.PENDING)
                    .orElse(null);
        }

        // LinkU 기록이 아예 없는 방
        if (latest == null) {
            return new LinkuStateRes(false, false, null, null);
        }

        boolean linked = latest.getStatus() == LinkuStatus.ACCEPTED;

        boolean canReview = false;
        if (linked && current.getId().equals(latest.getTarget().getId())) {
            boolean alreadyReviewed =
                    reviewRepository.existsByConnection_IdAndReviewer(latest.getId(), current);
            canReview = !alreadyReviewed;
        }

        // 프론트에서 connectionId, status 를 계속 쓸 수 있도록 내려줌
        return new LinkuStateRes(
                linked,
                canReview,
                latest.getId(),
                latest.getStatus().name()
        );
    }

    // ===== LinkU 제안 =====
    @Transactional
    public LinkuStateRes propose(Long roomId, String requesterLoginId,
                                 Long targetUserId, String message, Long talentPostId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("room not found"));

        User requester = findUserByUserId(requesterLoginId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("target user not found"));

        LinkuConnection conn = new LinkuConnection();
        conn.setRoom(room);
        conn.setRequester(requester);
        conn.setTarget(target);
        conn.setStatus(LinkuStatus.PENDING);
        conn.setCompleted(false);

        // 🔹 지금 보고 있는 게시글 기준으로 설정
        if (talentPostId != null) {
            var post = talentPostRepository.findById(talentPostId)
                    .orElseThrow(() -> new EntityNotFoundException("talent post not found: " + talentPostId));
            conn.setTalentPost(post);
        } else if (room.getPostId() != null) {
            // (백워드 호환용) 혹시 body에 없으면 방의 postId라도 대입
            talentPostRepository.findById(room.getPostId())
                    .ifPresent(conn::setTalentPost);
        }

        connectionRepository.save(conn);

        // 채팅방에 LinkU 제안 카드 메시지 쏘기
        chatService.sendLinkuProposeMessage(conn, message);

        return new LinkuStateRes(false, false, conn.getId(), conn.getStatus().name());
    }

    // ===== LinkU 수락 =====
    @Transactional
    public LinkuStateRes accept(Long linkuId, String currentUserLoginId) {
        LinkuConnection conn = connectionRepository.findById(linkuId)
                .orElseThrow(() -> new EntityNotFoundException("linku not found"));

        User current = findUserByUserId(currentUserLoginId);
        if (!current.getId().equals(conn.getTarget().getId())) {
            throw new IllegalStateException("수락 권한이 없습니다.");
        }

        conn.setStatus(LinkuStatus.ACCEPTED);
        conn.setCompleted(false); // 수락 시점에는 진행중 상태
        if (conn.getAcceptedAt() == null) {
            conn.setAcceptedAt(LocalDateTime.now());
        }

        // 채팅방에 "수락됨" 공지 메시지
        chatService.sendLinkuStatusMessage(conn, true);

        return new LinkuStateRes(true, true, conn.getId(), conn.getStatus().name());
    }

    // ===== LinkU 거절 =====
    @Transactional
    public void reject(Long linkuId, String currentUserLoginId) {
        LinkuConnection conn = connectionRepository.findById(linkuId)
                .orElseThrow(() -> new EntityNotFoundException("linku not found"));

        User current = findUserByUserId(currentUserLoginId);
        if (!current.getId().equals(conn.getTarget().getId())) {
            throw new IllegalStateException("거절 권한이 없습니다.");
        }

        conn.setStatus(LinkuStatus.REJECTED);
        conn.setCompleted(false); // 거절은 애초에 진행 X (통계에는 포함 안 됨)

        // 채팅방에 "거절됨" 공지 메시지
        chatService.sendLinkuStatusMessage(conn, false);
    }

    // ===== 리뷰 작성 =====
    @Transactional
    public void writeReview(Long roomId, String currentUserLoginId, LinkuReviewReq req) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("room not found"));

        // 가장 최근 ACCEPTED LinkU 기준으로 후기 작성
        LinkuConnection conn = connectionRepository
                .findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(room.getId(), LinkuStatus.ACCEPTED)
                .orElseThrow(() -> new IllegalStateException("수락된 LinkU가 없습니다."));

        User reviewer = findUserByUserId(currentUserLoginId);
        if (!reviewer.getId().equals(conn.getTarget().getId())) {
            // 현재 설계: LinkU 제안을 받은 쪽(target)만 리뷰를 남길 수 있음
            throw new IllegalStateException("리뷰 권한이 없습니다.");
        }

        if (reviewRepository.existsByConnection_IdAndReviewer(conn.getId(), reviewer)) {
            throw new IllegalStateException("이미 후기를 작성했습니다.");
        }

        LinkuReview review = new LinkuReview();
        review.setConnection(conn);
        review.setReviewer(reviewer);
        review.setTarget(conn.getRequester()); // 제안한 사람에게 남기는 후기
        review.setRelationRating(RelationRating.valueOf(req.getRelationRating()));
        review.setKindnessScore(req.getKindnessScore());
        review.setContent(req.getContent());

        reviewRepository.save(review);

        // ✅ 이 LinkU 협업을 '완료'로 표시
        conn.setCompleted(true);

        // 후기 작성 완료 후 채팅방에 공지 메시지 쏘기
        chatService.sendReviewNoticeMessage(conn, review);
    }

    // ===== 내가 받은 리뷰 목록 조회 =====
    @Transactional(readOnly = true)
    public List<LinkuReviewRes> getMyReviews(String currentUserLoginId) {
        User me = findUserByUserId(currentUserLoginId);
        List<LinkuReview> list = reviewRepository.findByTargetOrderByCreatedAtDesc(me);
        return toReviewResList(list);
    }

    // ===== 특정 유저(userId)가 받은 리뷰 목록 조회 (프로필용) =====
    @Transactional(readOnly = true)
    public List<LinkuReviewRes> getUserReviewsByLoginId(String userLoginId) {
        User target = findUserByUserId(userLoginId);
        List<LinkuReview> list = reviewRepository.findByTargetOrderByCreatedAtDesc(target);
        return toReviewResList(list);
    }

    // ✅ 내 링크유 목록 조회
    @Transactional(readOnly = true)
    public List<LinkuMyConnectionRes> getMyConnections(String currentUserLoginId) {
        User me = findUserByUserId(currentUserLoginId);
        Long myId = me.getId(); // 지금은 아래에서 안 쓰지만, 혹시 나중에 쓸 수 있어서 남겨둠

        List<LinkuConnection> conns = connectionRepository.findCompletedByUserId(myId);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        return conns.stream().map(c -> {
            // 제안자(항상 보낸 사람)
            User requester = c.getRequester();
            // 제안을 받은 사람(항상 받은 사람)
            User target = c.getTarget();

            // 👉 앞쪽 아바타: 항상 requester(보낸 사람)
            Long proposerId = requester.getId();
            String proposerName = safeName(requester);
            String proposerProfile = requester.getProfileImageUrl();

            // 👉 뒤쪽 아바타: 항상 target(받은 사람)
            Long partnerId = target.getId();
            String partnerName = safeName(target);
            String partnerProfile = target.getProfileImageUrl();

            // 재능 정보 (ChatRoom 은 postId 만 가지고 있음)
            // 재능 정보
            Long postId = null;
            String postTitle = null;

// 🔹 1순위: LinkU 자체에 저장된 게시글
            if (c.getTalentPost() != null) {
                postId = c.getTalentPost().getId();
                postTitle = c.getTalentPost().getTitle();
            }
// 🔹 2순위: 예전 데이터(필드 없을 때)는 room.postId 로 fallback
            else if (c.getRoom() != null && c.getRoom().getPostId() != null) {
                postId = c.getRoom().getPostId();
                var postOpt = talentPostRepository.findById(postId);
                if (postOpt.isPresent()) {
                    postTitle = postOpt.get().getTitle();
                }
            }

            // 날짜
            LocalDateTime acceptedAt = c.getAcceptedAt() != null
                    ? c.getAcceptedAt()
                    : c.getCreatedAt();
            String start = acceptedAt != null
                    ? acceptedAt.toLocalDate().format(dateFmt)
                    : null;

            var reviewOpt = reviewRepository.findFirstByConnection_IdOrderByCreatedAtDesc(c.getId());
            String end = reviewOpt.isPresent() && reviewOpt.get().getCreatedAt() != null
                    ? reviewOpt.get().getCreatedAt().toLocalDate().format(dateFmt)
                    : null;

            String periodTxt;
            if (start != null && end != null) {
                periodTxt = start + " ~ " + end;
            } else if (start != null) {
                periodTxt = start + " ~ 진행중";
            } else {
                periodTxt = null;
            }

            return new LinkuMyConnectionRes(
                    c.getId(),
                    (c.getRoom() != null ? c.getRoom().getId() : null),
                    proposerId,
                    proposerName,
                    proposerProfile,
                    partnerId,
                    partnerName,
                    partnerProfile,
                    postId,
                    postTitle,
                    start,
                    end,
                    periodTxt
            );
        }).collect(Collectors.toList());
    }

    private String safeName(User u) {
        String name = u.getUsername();
        if (name == null || name.isBlank()) {
            name = u.getUserId();
        }
        return name;
    }

    // 공통 변환 로직 (리뷰 -> 응답 DTO)
    private List<LinkuReviewRes> toReviewResList(List<LinkuReview> list) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return list.stream()
                .map(r -> {
                    String displayName = r.getReviewer().getUsername();
                    if (displayName == null || displayName.isBlank()) {
                        displayName = r.getReviewer().getUserId(); // username 없으면 아이디 fallback
                    }
                    String major = r.getReviewer().getMajor();

                    return new LinkuReviewRes(
                            r.getId(),
                            r.getRelationRating().name(),
                            r.getKindnessScore(),
                            r.getContent(),
                            displayName,
                            major,
                            (r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : null)
                    );
                })
                .collect(Collectors.toList());
    }

    // ===== 리뷰 삭제 (보낸/받은 둘 다) =====
    @Transactional
    public void deleteReview(Long reviewId, String currentUserLoginId) {
        User current = findUserByUserId(currentUserLoginId);

        LinkuReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("review not found"));

        boolean isReviewer = review.getReviewer().getId().equals(current.getId());
        boolean isTarget = review.getTarget().getId().equals(current.getId());

        if (!isReviewer && !isTarget) {
            throw new IllegalStateException("이 후기를 삭제할 권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }

    // ===== 내 별점 평균 / 리뷰 개수 =====
    @Transactional(readOnly = true)
    public LinkuRatingSummaryRes getMyRatingSummary(String currentUserLoginId) {
        User me = findUserByUserId(currentUserLoginId);
        return buildRatingSummaryForUser(me);
    }

    // ===== 특정 유저(타인) 별점 평균 / 리뷰 개수 (PK 기준) =====
    @Transactional(readOnly = true)
    public LinkuRatingSummaryRes getUserRatingSummary(Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("user not found: " + targetUserId));
        return buildRatingSummaryForUser(target);
    }

    // ===== 특정 유저(타인) 별점 평균 / 리뷰 개수 (로그인 아이디 기준) =====
    @Transactional(readOnly = true)
    public LinkuRatingSummaryRes getUserRatingSummaryByLoginId(String userLoginId) {
        User target = findUserByUserId(userLoginId);
        return buildRatingSummaryForUser(target);
    }

    // ===== 공통 별점 요약 로직 =====
    private LinkuRatingSummaryRes buildRatingSummaryForUser(User target) {
        Long userId = target.getId();

        Double avg = reviewRepository.findAverageKindnessScoreByTargetId(userId);
        long reviewCount = reviewRepository.countByTarget_Id(userId);

        if (avg == null) {
            avg = 0.0;
        }
        double rounded = Math.round(avg * 10) / 10.0; // 소수점 1자리

        // ✅ 진행 중인 협업 수
        long ongoingCount = connectionRepository.countOngoingByUserId(userId);

        // ✅ 진행한 협업 수 (ACCEPTED 전체)
        long acceptedCount = connectionRepository.countAcceptedByUserId(userId);

        return new LinkuRatingSummaryRes(rounded, reviewCount, ongoingCount, acceptedCount);
    }

    // ===== 유저 조회 헬퍼 =====
    private User findUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("user not found " + userId));
    }
}
