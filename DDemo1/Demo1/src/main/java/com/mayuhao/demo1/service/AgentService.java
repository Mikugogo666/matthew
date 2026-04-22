package com.mayuhao.demo1.service;

import com.mayuhao.demo1.dto.ChatRequest;
import com.mayuhao.demo1.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AgentService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatClient chatClient;
    private final KnowledgeService knowledgeService;
    private final boolean mockMode;

    public AgentService(
            ChatClient chatClient,
            KnowledgeService knowledgeService,
            @Value("${spring.ai.openai.api-key:demo-key}") String apiKey) {
        this.chatClient = chatClient;
        this.knowledgeService = knowledgeService;
        this.mockMode = !StringUtils.hasText(apiKey) || "demo-key".equals(apiKey);
    }

    public ChatResponse chat(ChatRequest request) {
        String message = request.message().trim();

        if (message.equalsIgnoreCase("/time")) {
            return new ChatResponse(
                    request.conversationId(),
                    "tool",
                    "本地时间工具结果：" + LocalDateTime.now().format(TIME_FORMATTER),
                    List.of()
            );
        }

        if (message.toLowerCase().startsWith("/weather ")) {
            String city = message.substring("/weather ".length()).trim();
            if (city.isBlank()) {
                city = "未指定城市";
            }
            return new ChatResponse(
                    request.conversationId(),
                    "tool",
                    city + " 天气工具结果：晴，22°C，东南风 2 级。当前为本地默认结果，可替换为真实天气接口。",
                    List.of()
            );
        }

        List<Document> citations = request.useKnowledge()
                ? knowledgeService.search(message)
                : List.of();

        if (mockMode) {
            return new ChatResponse(
                    request.conversationId(),
                    "mock",
                    mockReply(message, citations),
                    extractCitations(citations)
            );
        }

        String knowledgeContext = citations.isEmpty()
                ? "当前没有召回到知识库片段，请基于通用知识谨慎作答。"
                : buildKnowledgeContext(citations);

        String answer = chatClient.prompt()
                .system("""
                        你是一个用于处理知识问答与工单场景的中文助手。
                        回答要简洁、实用。
                        如果提供了知识库片段，优先基于片段回答，不要编造来源。
                        """)
                .user(u -> u.text("""
                        用户问题：
                        {message}

                        知识库片段：
                        {knowledge}
                        """)
                        .param("message", message)
                        .param("knowledge", knowledgeContext))
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, request.conversationId()))
                .call()
                .content();

        return new ChatResponse(
                request.conversationId(),
                "real-model",
                answer,
                extractCitations(citations)
        );
    }

    private String mockReply(String message, List<Document> citations) {
        if (message.contains("自我介绍")) {
            return """
                    大家好，我叫马宇豪，就读于河北师范大学计算机科学与技术专业。
                    我的主技能是 Java，目前重点学习 AI Agent 方向，自己完成了基于 HttpClient 的智能问答程序、
                    Spring Boot + Spring AI 知识库工单助手等项目，正在持续补齐 RAG、Prompt 和模型接入能力。
                    """;
        }

        if (message.contains("项目") || message.contains("Spring AI")) {
            return """
                    这个项目围绕知识导入、智能聊天和工单分析三条主线构建。
                    我使用 Spring Boot 组织接口，结合 Spring AI 完成模型调用、多轮上下文和简易 RAG。
                    整体目标是让系统能够围绕具体业务问题给出更稳定、可追溯的输出。
                    """;
        }

        if (!citations.isEmpty()) {
            return "当前处于 Mock Mode。已结合召回的知识片段生成回答，你可以继续接入真实模型流程。";
        }

        return """
                当前处于 Mock Mode，没有读取真实模型接口。
                这个接口当前用于支撑聊天、上下文维护和知识库问答流程。
                """;
    }

    private String buildKnowledgeContext(List<Document> citations) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < citations.size(); i++) {
            Document document = citations.get(i);
            String title = String.valueOf(document.getMetadata().getOrDefault("title", "unknown"));
            builder.append("片段").append(i + 1).append("（").append(title).append("）：\n")
                    .append(document.getText() == null ? "" : document.getText())
                    .append("\n\n");
        }
        return builder.toString();
    }

    private List<String> extractCitations(List<Document> citations) {
        return citations.stream()
                .map(document -> String.valueOf(document.getMetadata().getOrDefault("title", "unknown")))
                .distinct()
                .toList();
    }
}
