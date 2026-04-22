package com.mayuhao.demo1.dto;

import java.util.List;

public record TicketResponse(
        Long id,
        String customerName,
        String question,
        String category,
        String priority,
        String summary,
        String replyDraft,
        String mode,
        List<String> citations
) {
}
