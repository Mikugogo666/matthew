package com.mayuhao.demo1.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketCreateRequest(
        @NotBlank String customerName,
        @NotBlank String question,
        boolean useKnowledge
) {
}
