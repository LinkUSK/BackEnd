// com.example.demo.dto.talent.TalentPostResponse
package com.example.demo.dto.talent;

import com.example.demo.entity.TalentCategory;
import java.time.LocalDateTime;
import java.util.List;

public record TalentPostResponse(
        Long id,
        String title,
        String content,
        TalentCategory category,

        // 태그
        List<Long> tagIds,
        List<String> tagNames,

        String extraNote,

        // 대표 이미지(첫장)
        String portfolioImageUrl,

        // 🔹 여러 장 이미지
        List<String> portfolioImageUrls,

        Integer price,
        String location,

        // 작성자 정보
        String authorUserId,
        String authorName,
        String authorMajor,
        String authorProfileImageUrl,

        Long views,
        Long likesCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
