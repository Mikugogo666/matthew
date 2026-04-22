package com.mayuhao.demo1.dto;

import java.util.List;

public record ChatResponse(
        String conversationId,
        String mode,
        String answer,
        List<String> citations
) {
}
