package com.example.demo.dto;

import jakarta.validation.constraints.Size;

// 모두 선택값으로 두고 변경된 것만 반영
public record UpdateMeRequest(
        @Size(min = 2, max = 30) String username,
        @Size(max = 100) String major,
        @Size(max = 255) String profileImageUrl
) {}
