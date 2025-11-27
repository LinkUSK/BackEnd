// 채팅방, 채팅 메시지 관련 비즈니스 로직 담당
package com.example.demo.chat;

import com.example.demo.chat.ChatMessage.MessageKind;
import com.example.demo.chat.LinkuConnection.LinkuStatus;
import com.example.demo.chat.dto.ChatMessageRes;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository roomRepo;            // 채팅방 DB
    private final ChatMessageRepository msgRepo;          // 메시지 DB
    private final ChatRoomExitRepository exitRepo;        // 방 나간 기록 DB
    private final LinkuConnectionRepository connectionRepo; // LinkU 정보 DB

    // STOMP를 이용해 메시지를 브라우저로 보내는 도구
    private final SimpMessagingTemplate template;

    // ================= 채팅방 생성/조회 =================

    /**
     * 같은 두 유저 사이에는 채팅방을 1개만 유지
     * - postId는 "처음 어떤 글에서 대화가 시작됐는지" 참고용으로만 저장
     */
    @Transactional
    public ChatRoom getOrCreateRoom(Long postId, Long ownerId, Long otherUserId) {
        // 두 유저가 주인이든, 상대든 순서 상관 없이 기존 방이 있으면 재사용
        return roomRepo
                .findByOwnerIdAndOtherUserIdOrOwnerIdAndOtherUserId(
                        ownerId, otherUserId,
                        otherUserId, ownerId
                )
                .orElseGet(() ->
                        // 없으면 새 방 만들기
                        roomRepo.save(ChatRoom.builder()
                                .postId(postId)       // 어떤 글에서 대화 시작했는지 기록
                                .ownerId(ownerId)
                                .otherUserId(otherUserId)
                                .build())
                );
    }

    // ================= 메시지 저장 =================

    /**
     * 일반 텍스트 메시지 저장
     */
    @Transactional
    public ChatMessage saveMessage(Long roomId, Long senderId, Long receiverId, String content) {
        return saveMessage(roomId, senderId, receiverId, content, MessageKind.TEXT, null);
    }

    /**
     * kind / linkuConnectionId 를 지정해서 저장하는 버전
     * - LinkU 제안/수락/거절/후기 알림도 이걸로 저장
     */
    @Transactional
    public ChatMessage saveMessage(
            Long roomId,
            Long senderId,
            Long receiverId,
            String content,
            MessageKind kind,
            Long linkuConnectionId
    ) {
        // 새 메시지 엔티티 생성 후 저장
        return msgRepo.save(ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .createdAt(Instant.now())       // 현재 시간
                .readFlag(false)               // 처음엔 항상 안 읽은 상태
                .kind(kind != null ? kind : MessageKind.TEXT) // 메시지 종류
                .linkuConnectionId(linkuConnectionId)          // 연결된 LinkU ID
                .build());
    }

    // ================= 메시지 조회 (이전 기록 숨기기) =================

    /**
     * 최근 메시지 목록 (내 기준)
     * - 내가 이 방을 나간 기록이 있으면 → 그 시점 이후 메시지만
     * - 나간 적이 없으면 → 방의 전체 메시지
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> last50ForUser(Long roomId, Long userId) {
        // 내가 마지막으로 언제 이 방을 나갔는지 조회
        var exitOpt = exitRepo.findTopByRoomIdAndUserIdOrderByExitedAtDesc(roomId, userId);

        if (exitOpt.isPresent()) {
            Instant exitedAt = exitOpt.get().getExitedAt();
            // 나간 이후의 메시지만 시간 순으로 가져오기
            return msgRepo.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, exitedAt);
        } else {
            // 나간 적이 없으면 방의 전체 메시지를 가져옴
            return msgRepo.findByRoomIdOrderByCreatedAtAsc(roomId);
        }
    }

    /**
     * 메시지 목록 + 각 메시지의 LinkU 상태 포함해서 DTO로 반환
     * + 이 함수가 불릴 때, 내가 받은 메시지는 모두 "읽음 처리" 함
     */
    @Transactional
    public List<ChatMessageRes> last50ForUserWithLinkuState(Long roomId, Long userId) {
        // 1) 방 기준 메시지들 (내가 나간 후의 것만)
        List<ChatMessage> list = last50ForUser(roomId, userId);

        // 2) 이 방에서 내가 받은 메시지들을 모두 읽음 처리
        msgRepo.markAsReadInRoom(roomId, userId);

        // 3) 각 메시지에 연결된 LinkU 상태를 찾아서 함께 DTO로 변환
        return list.stream()
                .map(m -> {
                    String status = null;
                    Long linkuId = m.getLinkuConnectionId();
                    if (linkuId != null) {
                        status = connectionRepo.findById(linkuId)
                                .map(c -> c.getStatus().name())
                                .orElse(null);
                    }
                    return ChatMessageRes.from(m, status);
                })
                .toList();
    }

    /**
     * 이 방이 "내 기준에서 완전히 나간 방인지" 판단하는 함수
     * - 나간 이후로 새 메시지가 전혀 없으면 true (목록에 안 보이게)
     */
    @Transactional(readOnly = true)
    public boolean isRoomHiddenForUser(Long roomId, Long userId) {
        var exitOpt = exitRepo.findTopByRoomIdAndUserIdOrderByExitedAtDesc(roomId, userId);
        if (exitOpt.isEmpty()) {
            // 한 번도 나간 적 없으면 항상 보여줌
            return false;
        }

        Instant exitedAt = exitOpt.get().getExitedAt();

        // 나간 시점 이후에 새 메시지가 하나라도 있으면 → 다시 대화가 시작된 것
        boolean hasNewMessages = msgRepo.existsByRoomIdAndCreatedAtAfter(roomId, exitedAt);
        return !hasNewMessages; // 새 메시지가 없으면 숨김
    }

    /**
     * 내 기준에서 채팅방 나가기
     * - 방 자체는 삭제하지 않음
     * - 나간 기록만 남겨서, 내 화면에서만 숨기거나 이전 메시지 안 보이게 처리
     */
    @Transactional
    public void leaveRoomForUser(Long roomId, Long userId) {
        ChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 이 사람이 방의 참가자인지 확인 (owner 또는 other 둘 중 하나여야 함)
        if (!room.getOwnerId().equals(userId) && !room.getOtherUserId().equals(userId)) {
            throw new IllegalArgumentException("이 채팅방의 참가자가 아닙니다.");
        }

        // 매번 새로운 나간 기록을 남김 → 가장 마지막 기록 기준으로 잘라냄
        exitRepo.save(ChatRoomExit.builder()
                .roomId(roomId)
                .userId(userId)
                .exitedAt(Instant.now())
                .build());
    }

    // ================= 안 읽은 메시지 개수 계산 =================

    /**
     * 특정 방에서, 내가 아직 읽지 않은 메시지가 몇 개인지 계산
     * - 내가 방을 나갔었다면 → 그 이후 메시지만 셈
     * - 안 나갔다면 → 전체 메시지 중 readFlag=false 인 것만 셈
     */
    @Transactional(readOnly = true)
    public int unreadCountForUserInRoom(Long roomId, Long userId) {
        var exitOpt = exitRepo.findTopByRoomIdAndUserIdOrderByExitedAtDesc(roomId, userId);

        long count;
        if (exitOpt.isPresent()) {
            Instant exitedAt = exitOpt.get().getExitedAt();
            count = msgRepo.countByRoomIdAndReceiverIdAndCreatedAtAfterAndReadFlagFalse(
                    roomId, userId, exitedAt
            );
        } else {
            count = msgRepo.countByRoomIdAndReceiverIdAndReadFlagFalse(roomId, userId);
        }
        // int 타입에 넣어야 해서 형 변환
        return (int) count;
    }

    // ================= LinkU용 유틸 (LinkuService에서 사용) =================

    /**
     * LinkU 제안 카드 메시지 저장 + STOMP 브로드캐스트
     * - LinkU를 제안할 때 채팅방에 "카드 형태" 메시지를 남김
     */
    @Transactional
    public ChatMessageRes sendLinkuProposeMessage(LinkuConnection connection, String messageContent) {
        Long roomId = connection.getRoom().getId();
        Long requesterId = connection.getRequester().getId();
        Long targetId = connection.getTarget().getId();

        String content = (messageContent != null && !messageContent.isBlank())
                ? messageContent
                : "함께 LinkU를 제안했습니다.";

        ChatMessage msg = saveMessage(
                roomId,
                requesterId,
                targetId,
                content,
                MessageKind.LINKU_PROPOSE,
                connection.getId()
        );

        ChatMessageRes res = ChatMessageRes.from(msg, connection.getStatus().name());
        // 이 방을 보고 있는 클라이언트들에게 메시지 전송
        template.convertAndSend("/topic/chat.room." + roomId, res);
        return res;
    }

    /**
     * LinkU 수락/거절 카드 메시지 저장 + 브로드캐스트
     * @param accepted true → 수락, false → 거절
     */
    @Transactional
    public ChatMessageRes sendLinkuStatusMessage(LinkuConnection connection, boolean accepted) {
        Long roomId = connection.getRoom().getId();
        Long requesterId = connection.getRequester().getId();
        Long targetId = connection.getTarget().getId();

        // 수락/거절 버튼을 누르는 사람은 항상 target
        Long senderId = targetId;
        Long receiverId = requesterId;

        String content;
        MessageKind kind;
        if (accepted) {
            content = "LinkU가 수락되었습니다.";
            kind = MessageKind.LINKU_ACCEPT;
        } else {
            content = "LinkU가 거절되었습니다.";
            kind = MessageKind.LINKU_REJECT;
        }

        ChatMessage msg = saveMessage(
                roomId,
                senderId,
                receiverId,
                content,
                kind,
                connection.getId()
        );

        String status = accepted ? LinkuStatus.ACCEPTED.name() : LinkuStatus.REJECTED.name();

        ChatMessageRes res = ChatMessageRes.from(msg, status);
        template.convertAndSend("/topic/chat.room." + roomId, res);
        return res;
    }

    /**
     * LinkU 후기 작성 알림 메시지 저장 + 브로드캐스트
     * - kind: REVIEW_NOTICE
     * - receiverId: 후기를 "받은" 사람
     */
    @Transactional
    public ChatMessageRes sendReviewNoticeMessage(LinkuConnection connection, LinkuReview review) {
        Long roomId = connection.getRoom().getId();
        Long reviewerId = review.getReviewer().getId();
        Long targetId = review.getTarget().getId();

        // 이름이 없으면 userId로 대체
        String displayName = review.getReviewer().getUsername();
        if (displayName == null || displayName.isBlank()) {
            displayName = review.getReviewer().getUserId();
        }

        String content = displayName + "님이 후기를 남겼습니다.";

        ChatMessage msg = saveMessage(
                roomId,
                reviewerId,
                targetId,
                content,
                MessageKind.REVIEW_NOTICE,
                connection.getId()
        );

        ChatMessageRes res = ChatMessageRes.from(msg, null);
        template.convertAndSend("/topic/chat.room." + roomId, res);
        return res;
    }
}
