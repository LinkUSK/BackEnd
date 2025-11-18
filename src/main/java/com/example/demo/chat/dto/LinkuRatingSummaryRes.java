package com.example.demo.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LinkuRatingSummaryRes {

    // 평균 별점 (0.0 ~ 5.0)
    private double averageScore;

    // 리뷰 개수
    private long reviewCount;

    // 진행 중인 협업 수 (ACCEPTED + completed = false)
    private long ongoingCount;

    // 진행한 협업 수 (LinkU 수락 총 횟수 = ACCEPTED 카운트)
    private long acceptedCount;
}
