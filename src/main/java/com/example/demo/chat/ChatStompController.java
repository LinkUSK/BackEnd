// src/main/java/com/example/demo/chat/ChatStompController.java
package com.example.demo.chat;

import com.example.demo.chat.dto.ChatMessageDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;
    private final UserRepository userRepository;

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageDto incoming, Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Unauthenticated STOMP connection");
        }

        // 🔹 principal.getName() 이 숫자(DB id)일 수도, userId(문자열) 일 수도 있으니 둘 다 처리
        Long senderId = resolveUserId(principal.getName());

        // receiverId, roomId는 프론트에서 넘어온 값을 그대로 사용
        if (incoming.getRoomId() == null || incoming.getReceiverId() == null) {
            throw new IllegalArgumentException("roomId/receiverId가 필요합니다.");
        }
        if (incoming.getReceiverId().equals(senderId)) {
            throw new IllegalArgumentException("본인에게는 보낼 수 없습니다.");
        }

        // ✅ TEXT 메시지 저장 (여기서 exit 기록도 같이 삭제됨)
        var saved = chatService.saveMessage(
                incoming.getRoomId(),
                senderId,
                incoming.getReceiverId(),
                incoming.getContent()
        );

        // 저장 결과로 브로드캐스트 (id, createdAt 채워진 상태)
        ChatMessageDto outgoing = new ChatMessageDto(
                saved.getId(),
                saved.getRoomId(),
                saved.getSenderId(),
                saved.getReceiverId(),
                saved.getContent(),
                saved.getCreatedAt().toString()
        );

        template.convertAndSend("/topic/chat.room." + saved.getRoomId(), outgoing);
    }

    /** STOMP Principal 이름을 DB의 User.id(Long) 로 변환 */
    private Long resolveUserId(String principalName) {
        // 1) 먼저 숫자로 시도 (DB id로 넣은 경우)
        Long id = tryParseLong(principalName);
        if (id != null) {
            return id;
        }

        // 2) 숫자가 아니면 userId(학교 아이디)라고 보고 User 조회
        User user = userRepository.findByUserId(principalName)
                .orElseThrow(() -> new IllegalStateException("유저를 찾을 수 없습니다: " + principalName));
        return user.getId();
    }

    private Long tryParseLong(String s) {
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}
