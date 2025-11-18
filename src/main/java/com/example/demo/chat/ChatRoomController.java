// src/main/java/com/example/demo/chat/ChatRoomController.java
package com.example.demo.chat;

import com.example.demo.chat.dto.ChatMessageRes;
import com.example.demo.chat.dto.ChatRoomListItem;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final ChatRoomRepository roomRepo;
    private final ChatMessageRepository msgRepo;

    /** JWT subject(userId 문자열) -> DB의 User.id(Long) */
    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        String userId = (String) auth.getPrincipal();
        User me = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return me.getId();
    }

    /** 방 생성/조회 */
    @PostMapping("/rooms")
    public ResponseEntity<?> createOrGetRoom(@RequestBody Map<String, Object> req) {
        Object postIdObj = req.get("postId");
        if (postIdObj == null) throw new IllegalArgumentException("postId가 필요합니다.");
        Long postId = (postIdObj instanceof Number)
                ? ((Number) postIdObj).longValue()
                : Long.valueOf(postIdObj.toString());

        Long ownerId = null;
        Object ownerIdObj = req.get("ownerId");
        if (ownerIdObj != null) {
            ownerId = (ownerIdObj instanceof Number)
                    ? ((Number) ownerIdObj).longValue()
                    : Long.valueOf(ownerIdObj.toString());
        } else {
            String ownerUserId = (String) req.get("ownerUserId");
            if (!StringUtils.hasText(ownerUserId)) {
                throw new IllegalArgumentException("ownerId 또는 ownerUserId가 필요합니다.");
            }
            ownerId = userRepository.findByUserId(ownerUserId)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalArgumentException("글 작성자 정보를 찾을 수 없습니다."));
        }

        Long meId = currentUserId();
        if (meId.equals(ownerId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "자기 자신과는 채팅할 수 없습니다."));
        }

        ChatRoom room = chatService.getOrCreateRoom(postId, ownerId, meId);
        return ResponseEntity.ok(Map.of(
                "roomId", room.getId(),
                "postId", room.getPostId(),
                "ownerId", room.getOwnerId(),
                "otherUserId", room.getOtherUserId()
        ));
    }

    /**
     * 🔹 최근 50개 메시지 (오래된 순, "내 관점" 기준)
     *  - 내가 이 방을 나갔다면 → 그 이후 메시지만
     *  - 안 나갔다면 → 전체에서 최근 50개
     *  - + 각 메시지마다 LinkU 상태(PENDING/ACCEPTED/REJECTED) 포함
     *  - ➕ 이 호출 시점에 "내가 받은 메시지"는 모두 읽음 처리
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageRes>> last50(@PathVariable Long roomId) {
        Long meId = currentUserId();

        List<ChatMessageRes> dto = chatService.last50ForUserWithLinkuState(roomId, meId);

        return ResponseEntity.ok(dto);
    }

    /**
     * 🔹 내 채팅방 목록
     *  - 내가 owner 또는 other 인 방들 중에서
     *  - "완전히 나간 상태"(나간 후 새 메시지가 없는 방)는 목록에서 제외
     *  - 각 방마다 unread(읽지 않은 메시지 개수) 포함
     */
    @GetMapping("/my-rooms")
    public ResponseEntity<List<ChatRoomListItem>> myRooms() {
        Long meId = currentUserId();

        // 내가 owner 또는 other 인 방만
        List<ChatRoom> rooms = roomRepo.findByOwnerIdOrOtherUserId(meId, meId);

        List<ChatRoomListItem> result = rooms.stream()
                // 완전히 나간 상태인 방만 숨김
                .filter(r -> !chatService.isRoomHiddenForUser(r.getId(), meId))
                .map(r -> {
                    Long otherId = r.getOwnerId().equals(meId)
                            ? r.getOtherUserId()
                            : r.getOwnerId();
                    User other = userRepository.findById(otherId).orElse(null);

                    var lastOpt = msgRepo.findTop1ByRoomIdOrderByCreatedAtDesc(r.getId());

                    // ✅ 이 방에서 내가 아직 읽지 않은 메시지 개수
                    int unread = chatService.unreadCountForUserInRoom(r.getId(), meId);

                    return ChatRoomListItem.builder()
                            .roomId(r.getId())
                            .otherUser(ChatRoomListItem.OtherUser.builder()
                                    .id(other != null ? other.getId() : null)
                                    .userId(other != null ? other.getUserId() : null)
                                    .name(other != null ? other.getUsername() : null)
                                    .major(other != null ? other.getMajor() : null)
                                    .avatar(other != null ? other.getProfileImageUrl() : null)
                                    .build())
                            .lastMessage(lastOpt.map(m -> ChatRoomListItem.LastMessage.builder()
                                    .content(m.getContent())
                                    .createdAt(m.getCreatedAt())
                                    .build()).orElse(null))
                            .unread(unread)
                            .build();
                })
                // 마지막 메시지 시각 기준 정렬 (최신 방이 위로)
                .sorted(Comparator.comparing(
                        (ChatRoomListItem it) -> it.getLastMessage() == null
                                ? null
                                : it.getLastMessage().getCreatedAt(),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * 🔹 채팅방 나가기 (내 계정에서만 나가기)
     *  - 상대방 입장에서는 그대로 방/기록 보임
     *  - 나는 /my-rooms 에서 안 보이고,
     *    /messages 에서는 "마지막으로 나간 이후" 메시지만 보임
     */
    @DeleteMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId) {
        Long meId = currentUserId();
        chatService.leaveRoomForUser(roomId, meId);
        return ResponseEntity.ok(Map.of("message", "left"));
    }
}
