// src/main/java/com/example/demo/chat/dto/ChatRoomListItem.java
package com.example.demo.chat.dto;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListItem {
    private Long roomId;
    private OtherUser otherUser;
    private LastMessage lastMessage;
    // ✅ 이제 실제 "읽지 않은 메시지 개수"가 들어감
    private int unread;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OtherUser {
        private Long id;
        private String userId;
        private String name;
        private String major;
        private String avatar; // /files/xxx
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LastMessage {
        private String content;
        private Instant createdAt;
    }
}
