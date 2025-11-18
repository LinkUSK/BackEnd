// src/main/java/com/example/demo/chat/dto/LinkuStateRes.java
package com.example.demo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LinkuStateRes {

    private boolean linked;        // LinkU 수락 여부
    private boolean canReview;     // 내가 후기 작성 가능 여부
    private Long connectionId;     // LinkuConnection PK
    private String status;         // PENDING / ACCEPTED / REJECTED
}
