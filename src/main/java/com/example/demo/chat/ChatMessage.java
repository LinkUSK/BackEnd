// src/main/java/com/example/demo/chat/ChatMessage.java
package com.example.demo.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "chat_message",
        indexes = @Index(name = "idx_room_created", columnList = "roomId,createdAt")
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 채팅방 id (ChatRoom.id)
    private Long roomId;

    // 보낸 사람
    private Long senderId;

    // 받는 사람
    private Long receiverId;

    @Column(length = 2000)
    private String content;

    // 생성 시각
    private Instant createdAt;

    // 읽음 여부: receiver가 이 메시지를 읽었으면 true
    private boolean readFlag;

    // ===== LinkU 관련 필드 =====

    /** 메시지 종류 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private MessageKind kind = MessageKind.TEXT;

    /** 이 메시지가 어떤 linku_connections 레코드를 가리키는지 (없으면 null) */
    @Column(name = "linku_connection_id")
    private Long linkuConnectionId;

    public enum MessageKind {
        TEXT,           // 일반 채팅
        LINKU_PROPOSE,  // LinkU 제안 카드
        LINKU_ACCEPT,   // LinkU 수락 공지
        LINKU_REJECT,   // LinkU 거절 공지
        REVIEW_NOTICE   // 후기 남김 공지
    }
}
