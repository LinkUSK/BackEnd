// src/main/java/com/example/demo/chat/dto/LinkuProposeReq.java
package com.example.demo.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkuProposeReq {

    private Long targetUserId; // 수락 받을 유저 PK
    private String message;    // 카드에 보여줄 텍스트 (선택)

    // 🔹 이 LinkU가 어떤 재능글 기준인지
    private Long talentPostId;
}
