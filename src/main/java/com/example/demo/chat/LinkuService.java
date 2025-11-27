// LinkU(협업) 상태, 리뷰, 별점 요약 등을 관리하는 서비스
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

    private final ChatRoomRepository chatRoomRepository;           // 채팅방 DB
    private final UserRepository userRepository;                   // 유저 DB
    private final LinkuConnectionRepository connectionRepository;  // LinkU 연결 DB
    private final LinkuReviewRepository reviewRepository;          // LinkU 후기 DB
    private final ChatService chatService;                         // 채팅 알림용
    private final TalentPostRepository talentPostRepository;       // 재능 글 DB

    // ===== LinkU 상태 조회 =====
    @Transactional(readOnly = true)
    public LinkuStateRes getState(Long roomId, String currentUserLoginId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("room not found"));

        User current = findUserByUserId(currentUserLoginId);

        // 1순위: ACCEPTED 상태인 것 중 가장 최신
        LinkuConnection latest = connectionRepository
                .findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(room.getId(), LinkuStatus.ACCEPTED)
                .orElse(null);

        // 2순위: ACCEPTED가 없으면 PENDING 상태 중 가장 최신
        if (latest == null) {
            latest = connectionRepository
                    .findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(room.getId(), LinkuStatus.PENDING)
                    .orElse(null);
        }

        // LinkU 기록 자체가 없으면, 아직 LinkU를 한 적 없는 방
        if (latest == null) {
            return new LinkuStateRes(false, false, null, null);
        }

        // ACCEPTED 상태면 실제로 연결된 상태
        boolean linked = latest.getStatus() == LinkuStatus.ACCEPTED;

        // 내가 리뷰를 쓸 수 있는지 여부
        boolean canReview = false;
        if (linked && current.getId().equals(latest.getTarget().getId())) {
            // 현재 설계: 제안을 받은 사람(target)만 리뷰 작성 가능
            boolean alreadyReviewed =
                    reviewRepository.existsByConnection_IdAndReviewer(latest.getId(), current);
            canReview = !alreadyReviewed;
        }

        // 프론트에서 계속 사용할 수 있도록 connectionId, status 내려줌
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

        User requester = findUserByUserId(requesterLoginId); // 제안 보낸 사람
        User target = userRepository.findById(targetUserId)   // 제안 받는 사람
                .orElseThrow(() -> new EntityNotFoundException("target user not found"));

        // 새 LinkU 연결 엔티티 생성
        LinkuConnection conn = new LinkuConnection();
        conn.setRoom(room);
        conn.setRequester(requester);
        conn.setTarget(target);
        conn.setStatus(LinkuStatus.PENDING); // 처음에는 대기 상태
        conn.setCompleted(false);            // 아직 완료 아님

        // 지금 보고 있는 재능 글 정보를 연결
        if (talentPostId != null) {
            var post = talentPostRepository.findById(talentPostId)
                    .orElseThrow(() -> new EntityNotFoundException("talent post not found: " + talentPostId));
            conn.setTalentPost(post);
        } else if (room.getPostId() != null) {
            // 예전 데이터: room 안에만 postId가 있던 시절 대비
            talentPostRepository.findById(room.getPostId())
                    .ifPresent(conn::setTalentPost);
        }

        connectionRepository.save(conn);

        // 채팅방에 LinkU 제안 카드 메시지 전송
        chatService.sendLinkuProposeMessage(conn, message);

        return new LinkuStateRes(false, false, conn.getId(), conn.getStatus().name());
    }

    // ===== LinkU 수락 =====
    @Transactional
    public LinkuStateRes accept(Long linkuId, String currentUserLoginId) {
        LinkuConnection conn = connectionRepository.findById(linkuId)
                .orElseThrow(() -> new EntityNotFoundException("linku not found"));

        User current = findUserByUserId(currentUserLoginId);

        // 수락 권한 체크: LinkU 제안을 받은 사람만 가능
        if (!current.getId().equals(conn.getTarget().getId())) {
            throw new IllegalStateException("수락 권한이 없습니다.");
        }

        conn.setStatus(LinkuStatus.ACCEPTED);  // 상태를 ACCEPTED로 변경
        conn.setCompleted(false);              // 수락 이후에는 진행 중 상태
        if (conn.getAcceptedAt() == null) {
            conn.setAcceptedAt(LocalDateTime.now()); // 수락 시간 기록
        }

        // 채팅방에 "수락됨" 메시지 보내기
        chatService.sendLinkuStatusMessage(conn, true);

        return new LinkuStateRes(true, true, conn.getId(), conn.getStatus().name());
    }

    // ===== LinkU 거절 =====
    @Transactional
    public void reject(Long linkuId, String currentUserLoginId) {
        LinkuConnection conn = connectionRepository.findById(linkuId)
                .orElseThrow(() -> new EntityNotFoundException("linku not found"));

        User current = findUserByUserId(currentUserLoginId);
        // 거절도 제안을 받은 사람만 가능
        if (!current.getId().equals(conn.getTarget().getId())) {
            throw new IllegalStateException("거절 권한이 없습니다.");
        }

        conn.setStatus(LinkuStatus.REJECTED);
        conn.setCompleted(false); // 거절된 건 협업 진행에 포함하지 않음

        // 채팅방에 "거절됨" 메시지 보내기
        chatService.sendLinkuStatusMessage(conn, false);
    }

    // ===== 리뷰 작성 =====
    @Transactional
    public void writeReview(Long roomId, String currentUserLoginId, LinkuReviewReq req) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("room not found"));

        // 가장 최근 ACCEPTED 상태인 LinkU 가져오기
        LinkuConnection conn = connectionRepository
                .findFirstByRoom_IdAndStatusOrderByCreatedAtDesc(room.getId(), LinkuStatus.ACCEPTED)
                .orElseThrow(() -> new IllegalStateException("수락된 LinkU가 없습니다."));

        User reviewer = findUserByUserId(currentUserLoginId);
        // 현재 설계: 제안을 받은 사람(target)만 후기를 남김
        if (!reviewer.getId().equals(conn.getTarget().getId())) {
            throw new IllegalStateException("리뷰 권한이 없습니다.");
        }

        // 이미 리뷰를 쓴 적 있으면 중복 작성 방지
        if (reviewRepository.existsByConnection_IdAndReviewer(conn.getId(), reviewer)) {
            throw new IllegalStateException("이미 후기를 작성했습니다.");
        }

        // 새 리뷰 엔티티 생성
        LinkuReview review = new LinkuReview();
        review.setConnection(conn);
        review.setReviewer(reviewer);              // 리뷰를 쓰는 사람
        review.setTarget(conn.getRequester());     // 리뷰를 받는 사람(제안자)
        review.setRelationRating(RelationRating.valueOf(req.getRelationRating()));
        review.setKindnessScore(req.getKindnessScore());
        review.setContent(req.getContent());

        reviewRepository.save(review);

        // 이 LinkU 협업을 완료 상태로 표시
        conn.setCompleted(true);

        // 후기 작성 알림 메시지 채팅방에 전송
        chatService.sendReviewNoticeMessage(conn, review);
    }

    // ===== 내가 받은 리뷰 목록 =====
    @Transactional(readOnly = true)
    public List<LinkuReviewRes> getMyReviews(String currentUserLoginId) {
        User me = findUserByUserId(currentUserLoginId);
        List<LinkuReview> list = reviewRepository.findByTargetOrderByCreatedAtDesc(me);
        return toReviewResList(list);
    }

    // ===== 특정 유저(userId)가 받은 리뷰 목록 (프로필용) =====
    @Transactional(readOnly = true)
    public List<LinkuReviewRes> getUserReviewsByLoginId(String userLoginId) {
        User target = findUserByUserId(userLoginId);
        List<LinkuReview> list = reviewRepository.findByTargetOrderByCreatedAtDesc(target);
        return toReviewResList(list);
    }

    // ===== 내 LinkU(협업) 목록 조회 =====
    @Transactional(readOnly = true)
    public List<LinkuMyConnectionRes> getMyConnections(String currentUserLoginId) {
        User me = findUserByUserId(currentUserLoginId);
        Long myId = me.getId();

        // 나와 관련된 완료된 LinkU 목록
        List<LinkuConnection> conns = connectionRepository.findCompletedByUserId(myId);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

        return conns.stream().map(c -> {
            // 제안 보낸 사람
            User requester = c.getRequester();
            // 제안 받은 사람
            User target = c.getTarget();

            // 앞쪽 아바타: 항상 제안자
            Long proposerId = requester.getId();
            String proposerName = safeName(requester);
            String proposerProfile = requester.getProfileImageUrl();

            // 뒤쪽 아바타: 항상 제안 받은 사람
            Long partnerId = target.getId();
            String partnerName = safeName(target);
            String partnerProfile = target.getProfileImageUrl();

            // 재능 글 정보
            Long postId = null;
            String postTitle = null;

            // 1순위: LinkU 엔티티에 직접 저장된 재능 글
            if (c.getTalentPost() != null) {
                postId = c.getTalentPost().getId();
                postTitle = c.getTalentPost().getTitle();
            }
            // 2순위: 예전 데이터는 room.postId 에서 가져오기
            else if (c.getRoom() != null && c.getRoom().getPostId() != null) {
                postId = c.getRoom().getPostId();
                var postOpt = talentPostRepository.findById(postId);
                if (postOpt.isPresent()) {
                    postTitle = postOpt.get().getTitle();
                }
            }

            // 협업 시작 날짜 (수락일이 있으면 수락일, 없으면 생성일)
            LocalDateTime acceptedAt = c.getAcceptedAt() != null
                    ? c.getAcceptedAt()
                    : c.getCreatedAt();
            String start = acceptedAt != null
                    ? acceptedAt.toLocalDate().format(dateFmt)
                    : null;

            // 마지막 리뷰 작성 날짜 기준으로 종료일 표시
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

    // 이름이 없으면 userId로 대신 보여주는 헬퍼
    private String safeName(User u) {
        String name = u.getUsername();
        if (name == null || name.isBlank()) {
            name = u.getUserId();
        }
        return name;
    }

    // 리뷰 엔티티 리스트를 응답 DTO 리스트로 변환
    private List<LinkuReviewRes> toReviewResList(List<LinkuReview> list) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return list.stream()
                .map(r -> {
                    String displayName = r.getReviewer().getUsername();
                    if (displayName == null || displayName.isBlank()) {
                        displayName = r.getReviewer().getUserId();
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

    // ===== 리뷰 삭제 (본인/상대방 둘 다 삭제 가능) =====
    @Transactional
    public void deleteReview(Long reviewId, String currentUserLoginId) {
        User current = findUserByUserId(currentUserLoginId);

        LinkuReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("review not found"));

        boolean isReviewer = review.getReviewer().getId().equals(current.getId());
        boolean isTarget = review.getTarget().getId().equals(current.getId());

        // 리뷰를 쓴 사람이나, 리뷰를 받은 사람만 삭제 가능
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

    // ===== 특정 유저(타인) 별점 요약 (PK 기준) =====
    @Transactional(readOnly = true)
    public LinkuRatingSummaryRes getUserRatingSummary(Long targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("user not found: " + targetUserId));
        return buildRatingSummaryForUser(target);
    }

    // ===== 특정 유저(타인) 별점 요약 (userId 기준) =====
    @Transactional(readOnly = true)
    public LinkuRatingSummaryRes getUserRatingSummaryByLoginId(String userLoginId) {
        User target = findUserByUserId(userLoginId);
        return buildRatingSummaryForUser(target);
    }

    // ===== 공통 별점 요약 로직 =====
    private LinkuRatingSummaryRes buildRatingSummaryForUser(User target) {
        Long userId = target.getId();

        // 친절 점수 평균
        Double avg = reviewRepository.findAverageKindnessScoreByTargetId(userId);
        long reviewCount = reviewRepository.countByTarget_Id(userId);

        if (avg == null) {
            avg = 0.0;
        }
        // 소수점 첫째 자리까지만 남기기
        double rounded = Math.round(avg * 10) / 10.0;

        // 진행 중인 협업 수
        long ongoingCount = connectionRepository.countOngoingByUserId(userId);

        // ACCEPTED 된 전체 협업 수
        long acceptedCount = connectionRepository.countAcceptedByUserId(userId);

        return new LinkuRatingSummaryRes(rounded, reviewCount, ongoingCount, acceptedCount);
    }

    // ===== 유저 조회 헬퍼 =====
    private User findUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("user not found " + userId));
    }
}
