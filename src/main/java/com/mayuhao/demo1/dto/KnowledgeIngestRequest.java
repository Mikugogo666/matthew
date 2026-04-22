package com.mayuhao.demo1.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeIngestRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
