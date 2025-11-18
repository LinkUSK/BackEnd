package com.example.demo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** STOMP로 주고받는 DTO (프론트 구조에 맞춤) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;          // 서버 생성
    private Long roomId;
    private Long senderId;    // 서버 채움
    private Long receiverId;
    private String content;
    private String createdAt; // ISO8601
}
