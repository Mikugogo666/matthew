package com.mayuhao.demo1.controller;

import com.mayuhao.demo1.dto.KnowledgeIngestRequest;
import com.mayuhao.demo1.dto.KnowledgeStatusResponse;
import com.mayuhao.demo1.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/text")
    public KnowledgeStatusResponse ingestText(@Valid @RequestBody KnowledgeIngestRequest request) {
        knowledgeService.ingestText(request.title(), request.content());
        return knowledgeService.status();
    }

    @GetMapping
    public KnowledgeStatusResponse status() {
        return knowledgeService.status();
    }
}
