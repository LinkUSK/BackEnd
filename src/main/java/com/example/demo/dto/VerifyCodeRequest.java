// com.example.demo.dto.VerifyCodeRequest
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequest(
        @NotBlank String email,
        @NotBlank String code,
        @NotBlank @Size(min=2, max=30) String username,
        @NotBlank @Size(min=8, max=64) String password,
        String major,                // 선택
        String profileImageUrl       // 선택 (/files/xxx)
) {}
