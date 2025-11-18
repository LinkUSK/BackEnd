// src/main/java/com/example/demo/chat/dto/LinkuReviewRes.java
package com.example.demo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LinkuReviewRes {

    private Long id;
    private String relationRating;   // BAD / GOOD / BEST
    private int kindnessScore;       // 1~5
    private String content;          // 후기 내용 (nullable)

    private String reviewerName;     // 후기를 쓴 사람의 이름(username) - 없으면 userId로 대체
    private String reviewerMajor;    // 후기를 쓴 사람의 전공 (nullable)

    private String createdAt;        // "yyyy-MM-dd HH:mm" 형식의 문자열
}
