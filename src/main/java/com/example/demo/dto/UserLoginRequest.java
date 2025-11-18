package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank String userId,
        @NotBlank String password
) {}
