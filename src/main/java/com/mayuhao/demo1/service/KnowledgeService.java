package com.mayuhao.demo1.service;

import com.mayuhao.demo1.dto.KnowledgeStatusResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
    private final List<Document> ingestedChunks = new ArrayList<>();
    private final boolean mockMode;

    public KnowledgeService(
            VectorStore vectorStore,
            @Value("${spring.ai.openai.api-key:demo-key}") String apiKey) {
        this.vectorStore = vectorStore;
        this.mockMode = !StringUtils.hasText(apiKey) || "demo-key".equals(apiKey);
    }

    public synchronized void ingestText(String title, String content) {
        Document source = new Document(content, Map.of("title", title, "source", "text"));
        List<Document> chunks = tokenTextSplitter.apply(List.of(source));
        ingestedChunks.addAll(chunks);
        if (!mockMode) {
            vectorStore.add(chunks);
        }
    }

    public synchronized KnowledgeStatusResponse status() {
        Set<String> titles = ingestedChunks.stream()
                .map(document -> String.valueOf(document.getMetadata().getOrDefault("title", "unknown")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new KnowledgeStatusResponse(ingestedChunks.size(), new ArrayList<>(titles));
    }

    public synchronized List<Document> search(String query) {
        if (ingestedChunks.isEmpty()) {
            return List.of();
        }
        if (mockMode) {
            return keywordSearch(query);
        }
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThresholdAll()
                .build();
        return vectorStore.similaritySearch(request);
    }

    private List<Document> keywordSearch(String query) {
        List<String> tokens = tokenize(query);
        List<Document> matches = ingestedChunks.stream()
                .map(document -> Map.entry(document, score(document.getText(), tokens)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Document, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();
        if (!matches.isEmpty()) {
            return matches;
        }
        return ingestedChunks.stream().limit(2).toList();
    }

    private int score(String text, List<String> tokens) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String lower = text.toLowerCase();
        int score = 0;
        for (String token : tokens) {
            if (!token.isBlank() && lower.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private List<String> tokenize(String query) {
        return List.of(query.toLowerCase()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " ")
                .trim()
                .split("\\s+"));
    }
}
