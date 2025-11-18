// com/example/demo/dto/talent/TalentPostUpdateRequest.java
package com.example.demo.dto.talent;

import com.example.demo.entity.TalentCategory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TalentPostUpdateRequest(
        @NotBlank @Size(min = 2, max = 80)
        String title,

        @NotBlank @Size(min = 2, max = 2000)
        String content,

        @NotNull
        TalentCategory category,

        Long tagId,
        List<Long> tagIds,

        @NotBlank @Size(min = 2, max = 1000)
        String extraNote,

        @Size(max = 300)
        String portfolioImageUrl,

        // ✅ 수정 시에도 여러 장 지원
        List<@Size(max = 300) String> portfolioImageUrls,

        @Min(0) @Max(10_000_000)
        Integer price,

        @Size(max = 100)
        String location,

        // ACTIVE, DELETED 등
        String status
) {}
