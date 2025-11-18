// com/example/demo/dto/talent/TalentPostCreateRequest.java
package com.example.demo.dto.talent;

import com.example.demo.entity.TalentCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TalentPostCreateRequest(
        @NotBlank @Size(min = 2, max = 80)
        String title,

        @NotBlank @Size(min = 2, max = 2000)
        String content,

        @NotNull
        TalentCategory category,

        // 대표 태그(옵션) – 없으면 tagIds 에서 첫 번째 사용
        Long tagId,

        // 다중 태그
        List<Long> tagIds,

        @NotBlank @Size(min = 2, max = 1000)
        String extraNote,

        // ✅ 단일 대표 이미지 (레거시용)
        @Size(max = 300)
        String portfolioImageUrl,

        // ✅ 여러 장 이미지 URL 리스트
        List<@Size(max = 300) String> portfolioImageUrls,

        @Min(0) @Max(10_000_000)
        Integer price,

        @Size(max = 100)
        String location
) {}
