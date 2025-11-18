package com.example.demo.dto.meta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagCreateRequest(
        @NotBlank String category,
        @NotBlank @Size(min = 1, max = 20) String name
) {}
