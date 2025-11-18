// src/main/java/com/example/demo/chat/dto/LinkuMyConnectionRes.java
package com.example.demo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LinkuMyConnectionRes {

    private Long connectionId;
    private Long roomId;

    // 제안한 사람 (앞쪽 프로필)
    private Long proposerId;
    private String proposerName;
    private String proposerProfileImageUrl;

    // 나 기준 상대방 정보 (타이틀에 들어가는 이름 + 뒤쪽 프로필)
    private Long partnerId;
    private String partnerName;
    private String partnerProfileImageUrl;

    // 재능 정보
    private Long talentPostId;
    private String talentTitle;

    // 기간
    private String startDate; // 예: 2024년 11월 11일
    private String endDate;   // 예: 2024년 11월 12일
    private String period;    // 예: "2024년 11월 11일 ~ 2024년 11월 12일"
}
