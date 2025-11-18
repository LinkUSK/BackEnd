// src/main/java/com/example/demo/chat/dto/LinkuReviewReq.java
package com.example.demo.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkuReviewReq {

    // BAD / GOOD / BEST
    private String relationRating;

    // 1~5
    private int kindnessScore;

    // 선택 사항
    private String content;
}
