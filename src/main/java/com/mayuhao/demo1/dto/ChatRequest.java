package com.mayuhao.demo1.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String conversationId,
        @NotBlank String message,
        boolean useKnowledge
) {
}
