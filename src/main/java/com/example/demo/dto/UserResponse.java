package com.example.demo.dto;

public record UserResponse(
        Long id,
        String username,
        String userId,
        String email,
        String major,
        String profileImageUrl,
        String createdAt
) {}
