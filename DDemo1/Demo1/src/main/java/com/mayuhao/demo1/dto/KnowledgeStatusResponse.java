package com.mayuhao.demo1.dto;

import java.util.List;

public record KnowledgeStatusResponse(
        int chunkCount,
        List<String> titles
) {
}
