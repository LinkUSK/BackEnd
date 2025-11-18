// src/main/java/com/example/demo/chat/dto/ChatSendReq.java
package com.example.demo.chat.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatSendReq {
    private Long roomId;
    private Long receiverId;
    private String content;
}
