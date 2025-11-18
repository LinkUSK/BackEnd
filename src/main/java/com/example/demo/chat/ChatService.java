// src/main/java/com/example/demo/chat/ChatService.java
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

    private final ChatRoomRepository roomRepo;
    private final ChatMessageRepository msgRepo;
    private final ChatRoomExitRepository exitRepo;
    private final LinkuConnectionRepository connectionRepo;   // LinkU 상태 조회용

    // STOMP 브로드캐스트 템플릿
    private final SimpMessagingTemplate template;

    // ================= 채팅방 생성/조회 =================

    /**
     * 같은 두 유저 쌍에 대해서는 방을 1개만 유지하기 위해:
     *  - postId는 "최초 생성 시점의 게시글 id" 정도로만 저장
     *  - 방을 찾을 때는 (ownerId, otherUserId) / (otherUserId, ownerId) 둘 다 검색
     */
    @Transactional
    public ChatRoom getOrCreateRoom(Long postId, Long ownerId, Long otherUserId) {
        // 같은 두 유저 사이에 이미 방이 있으면 (순서와 상관 없이) 재사용
        return roomRepo
                .findByOwnerIdAndOtherUserIdOrOwnerIdAndOtherUserId(
                        ownerId, otherUserId,
                        otherUserId, ownerId
                )
                .orElseGet(() ->
                        roomRepo.save(ChatRoom.builder()
                                .postId(postId)       // 최초 생성 기준 게시글 id 저장 (참고용)
                                .ownerId(ownerId)
                                .otherUserId(otherUserId)
                                .build())
                );
    }

    // ================= 메시지 저장 =================

    /** 일반 TEXT 메시지 저장 */
    @Transactional
    public ChatMessage saveMessage(Long roomId, Long senderId, Long receiverId, String content) {
        return saveMessage(roomId, senderId, receiverId, content, MessageKind.TEXT, null);
    }

    /**
     * kind / linkuConnectionId 를 지정해서 저장하고 싶은 경우를 위한 오버로드
     * - LinkU 제안/수락/거절/후기 공지 메시지도 이 메서드로 저장
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
        return msgRepo.save(ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .createdAt(Instant.now())
                .readFlag(false) // 처음에는 항상 "읽지 않음"
                .kind(kind != null ? kind : MessageKind.TEXT)
                .linkuConnectionId(linkuConnectionId)
                .build());
    }

    // ================= 메시지 조회 (나갔다 들어오면 이전 기록 숨기기) =================

    /**
     * 최근 50개 메시지 (오래된 순, "내 관점" 기준)
     *  - 내가 이 방을 나간 기록이 있으면 → 그 시점 이후 메시지만 보여줌
     *  - 나간 적이 없으면 → 전체에서 최근 50개
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> last50ForUser(Long roomId, Long userId) {
        var exitOpt = exitRepo.findTopByRoomIdAndUserIdOrderByExitedAtDesc(roomId, userId);

        if (exitOpt.isPresent()) {
            Instant exitedAt = exitOpt.get().getExitedAt();
            // 나간 시각 이후 전체 메시지 (오래된 순)
            return msgRepo.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, exitedAt);
        } else {
            // 한 번도 나간 적이 없으면 이 방의 전체 메시지 (오래된 순)
            return msgRepo.findByRoomIdOrderByCreatedAtAsc(roomId);
        }
    }

    /**
     * 최근 50개 메시지 + 각 메시지에 대한 LinkU 상태까지 포함해서 DTO 로 반환.
     * ➕ 이 호출 시점에, 이 방에서 내가 받은 메시지를 "읽음 처리(readFlag=true)" 한다.
     *
     * - /api/chat/rooms/{roomId}/messages 엔드포인트에서 사용.
     */
    @Transactional
    public List<ChatMessageRes> last50ForUserWithLinkuState(Long roomId, Long userId) {
        // 1) 방 기준 메시지들 조회 (나간 시점 이후만)
        List<ChatMessage> list = last50ForUser(roomId, userId);

        // 2) 내가 이 방에서 받은 메시지들 모두 읽음 처리
        msgRepo.markAsReadInRoom(roomId, userId);

        // 3) LinkU 상태 포함해서 DTO 변환
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
     * my-rooms 목록에서 필터링할 때 쓰는 유틸
     * - "완전히 나간 상태"(나간 후 새 메시지가 전혀 없는 방)면 true
     * - 그 외에는 false (목록에 보여줌)
     */
    @Transactional(readOnly = true)
    public boolean isRoomHiddenForUser(Long roomId, Long userId) {
        var exitOpt = exitRepo.findTopByRoomIdAndUserIdOrderByExitedAtDesc(roomId, userId);
        if (exitOpt.isEmpty()) {
            // 나간 적이 한 번도 없으면 무조건 표시
            return false;
        }

        Instant exitedAt = exitOpt.get().getExitedAt();

        // 나간 시각 이후에 메시지가 하나라도 있으면 → 다시 대화가 시작된 것 → 목록에 표시
        boolean hasNewMessages = msgRepo.existsByRoomIdAndCreatedAtAfter(roomId, exitedAt);
        return !hasNewMessages;
    }

    /**
     * 내 관점에서만 채팅방 나가기
     * - 방/메시지는 그대로 두고
     * - chat_room_exit 에만 기록해서
     *   내 리스트 + 내 메시지 조회에서만 숨김/잘라내기
     */
    @Transactional
    public void leaveRoomForUser(Long roomId, Long userId) {
        ChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 이 방의 참가자인지 확인 (owner 또는 other)
        if (!room.getOwnerId().equals(userId) && !room.getOtherUserId().equals(userId)) {
            throw new IllegalArgumentException("이 채팅방의 참가자가 아닙니다.");
        }

        // 매번 새 기록을 남겨서 "마지막 나간 시점" 기준으로 잘라내도록 함.
        exitRepo.save(ChatRoomExit.builder()
                .roomId(roomId)
                .userId(userId)
                .exitedAt(Instant.now())
                .build());
    }

    // ================= 🔹 unread 개수 계산 =================

    /**
     * 특정 방에서 "나(meId)가 아직 읽지 않은 메시지 개수" 반환.
     *  - 내가 이 방을 나갔었다면 → 마지막으로 나간 시점 이후의 메시지만 카운트
     *  - 안 나갔다면 → 전체 메시지 중에서 readFlag=false 인 것들 카운트
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
        // int 필드에 넣을 거라 캐스팅
        return (int) count;
    }

    // ================= LinkU용 유틸 (LinkuService에서 사용) =================

    /**
     * LinkU 제안 카드 메시지 저장 + STOMP 브로드캐스트
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
        template.convertAndSend("/topic/chat.room." + roomId, res);
        return res;
    }

    /**
     * LinkU 수락/거절 카드 메시지 저장 + STOMP 브로드캐스트
     *
     * @param accepted true → 수락, false → 거절
     */
    @Transactional
    public ChatMessageRes sendLinkuStatusMessage(LinkuConnection connection, boolean accepted) {
        Long roomId = connection.getRoom().getId();
        Long requesterId = connection.getRequester().getId();
        Long targetId = connection.getTarget().getId();

        // 수락/거절 동작을 수행한 사람은 항상 target 유저
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
     * 후기 남김 공지 메시지 저장 + STOMP 브로드캐스트
     * - kind: REVIEW_NOTICE
     * - receiverId: 후기를 "받은" 사람 (target)
     * - 이 메시지를 받은 쪽에서만 "후기 보러가기" 버튼을 띄우면 된다.
     */
    @Transactional
    public ChatMessageRes sendReviewNoticeMessage(LinkuConnection connection, LinkuReview review) {
        Long roomId = connection.getRoom().getId();
        Long reviewerId = review.getReviewer().getId();
        Long targetId = review.getTarget().getId();

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
