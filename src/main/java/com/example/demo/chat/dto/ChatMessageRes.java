// src/main/java/com/example/demo/chat/dto/ChatMessageRes.java
package com.example.demo.chat.dto;

import com.example.demo.chat.ChatMessage;
import com.example.demo.chat.ChatMessage.MessageKind;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRes {

    private Long id;
    private Long roomId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Instant createdAt;

    // ===== LinkU 관련 필드 =====
    private String kind;        // "TEXT", "LINKU_PROPOSE", "LINKU_ACCEPT", "LINKU_REJECT"
    private Long   linkuId;     // linku_connections.id
    private String linkuStatus; // PENDING / ACCEPTED / REJECTED (TEXT 인 경우 null 가능)

    /** 일반 메시지용 */
    public static ChatMessageRes from(ChatMessage m) {
        return from(m, null);
    }

    /** LinkU 상태까지 함께 내려줄 때 사용 */
    public static ChatMessageRes from(ChatMessage m, String linkuStatus) {
        return ChatMessageRes.builder()
                .id(m.getId())
                .roomId(m.getRoomId())
                .senderId(m.getSenderId())
                .receiverId(m.getReceiverId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .kind(m.getKind() != null ? m.getKind().name() : MessageKind.TEXT.name())
                .linkuId(m.getLinkuConnectionId())
                .linkuStatus(linkuStatus)
                .build();
    }
}
