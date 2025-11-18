// com.example.demo.dto.talent.TalentPostListItem.java
package com.example.demo.dto.talent;

import com.example.demo.entity.TalentCategory;
import java.time.LocalDateTime;
import java.util.List;

public record TalentPostListItem(
        Long id,
        String title,
        TalentCategory category,
        List<Long> tagIds,
        List<String> tagNames,
        Integer price,
        String location,
        String authorUserId,
        String authorName,
        // ▼ 추가
        String authorMajor,
        String authorProfileImageUrl,
        Long views,
        LocalDateTime createdAt
) {}
