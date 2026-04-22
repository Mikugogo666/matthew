package com.mayuhao.demo1.service;

import com.mayuhao.demo1.dto.TicketCreateRequest;
import com.mayuhao.demo1.dto.TicketResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TicketService {

    private final ChatClient chatClient;
    private final KnowledgeService knowledgeService;
    private final boolean mockMode;
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final List<TicketResponse> tickets = new ArrayList<>();

    public TicketService(
            ChatClient chatClient,
            KnowledgeService knowledgeService,
            @Value("${spring.ai.openai.api-key:demo-key}") String apiKey) {
        this.chatClient = chatClient;
        this.knowledgeService = knowledgeService;
        this.mockMode = !StringUtils.hasText(apiKey) || "demo-key".equals(apiKey);
    }

    public synchronized TicketResponse createTicket(TicketCreateRequest request) {
        List<Document> citations = request.useKnowledge()
                ? knowledgeService.search(request.question())
                : List.of();

        TicketAnalysis analysis = mockMode
                ? mockAnalyze(request.question(), citations)
                : realAnalyze(request.question(), citations);

        TicketResponse response = new TicketResponse(
                idGenerator.getAndIncrement(),
                request.customerName(),
                request.question(),
                analysis.category(),
                analysis.priority(),
                analysis.summary(),
                analysis.replyDraft(),
                mockMode ? "mock" : "real-model",
                extractCitations(citations)
        );

        tickets.add(response);
        return response;
    }

    public synchronized List<TicketResponse> listTickets() {
        return List.copyOf(tickets);
    }

    public synchronized TicketResponse getTicket(Long id) {
        return tickets.stream()
                .filter(ticket -> ticket.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }

    private TicketAnalysis mockAnalyze(String question, List<Document> citations) {
        String lower = question.toLowerCase();

        String category = "一般咨询";
        String priority = "中";

        if (containsAny(lower, "网络", "wifi", "登录", "账号", "密码", "系统故障",
                "network", "login", "password", "auth", "authentication", "system error")) {
            category = "技术支持";
            priority = "高";
        } else if (containsAny(lower, "退款", "缴费", "费用", "发票",
                "refund", "payment", "invoice", "fee", "billing")) {
            category = "费用问题";
            priority = "高";
        } else if (containsAny(lower, "快递", "物流", "发货", "送达",
                "delivery", "shipping", "logistics", "package")) {
            category = "物流进度";
            priority = "中";
        } else if (containsAny(lower, "投诉", "差评", "态度", "不满意",
                "complaint", "bad review", "unhappy", "dissatisfied")) {
            category = "投诉建议";
            priority = "高";
        } else if (containsAny(lower, "课程", "宿舍", "请假", "选课", "校园卡",
                "course", "dormitory", "leave", "campus card")) {
            category = "校园服务";
            priority = "中";
        }

        String summary = "用户问题主要涉及“" + category + "”场景，需要尽快给出明确处理建议。";

        String knowledgeHint = citations.isEmpty()
                ? "当前没有召回到知识库片段，建议人工进一步确认细节。"
                : "已结合知识库资料生成建议，可优先按引用内容处理。";

        String replyDraft = """
                您好，已收到您的问题。
                目前系统判断该问题属于“%s”，优先级为“%s”。
                %s
                建议先核对问题描述、时间点和相关账号/订单信息，如果需要，我可以继续为您补充更具体的处理步骤。
                """.formatted(category, priority, knowledgeHint);

        return new TicketAnalysis(category, priority, summary, replyDraft);
    }

    private TicketAnalysis realAnalyze(String question, List<Document> citations) {
        String knowledge = citations.isEmpty() ? "无" : buildKnowledgeContext(citations);

        String analysis = chatClient.prompt()
                .system("""
                        你是一个工单分析助手。
                        请根据用户问题输出四项内容，并严格按以下格式返回：
                        category=...
                        priority=...
                        summary=...
                        reply=...
                        category 取值尽量简洁，例如：技术支持、费用问题、物流进度、投诉建议、一般咨询、校园服务。
                        priority 取值为：高、中、低。
                        """)
                .user(u -> u.text("""
                        用户问题：
                        {question}

                        可参考的知识库片段：
                        {knowledge}
                        """)
                        .param("question", question)
                        .param("knowledge", knowledge))
                .call()
                .content();

        return parseAnalysis(analysis);
    }

    private TicketAnalysis parseAnalysis(String analysis) {
        Map<String, String> values = analysis.lines()
                .map(String::trim)
                .filter(line -> line.contains("="))
                .map(line -> line.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(
                        arr -> arr[0].trim().toLowerCase(),
                        arr -> arr[1].trim(),
                        (a, b) -> b
                ));

        return new TicketAnalysis(
                values.getOrDefault("category", "一般咨询"),
                values.getOrDefault("priority", "中"),
                values.getOrDefault("summary", "需要进一步确认问题细节。"),
                values.getOrDefault("reply", "您好，已收到您的问题，我们会尽快处理。")
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private record TicketAnalysis(
            String category,
            String priority,
            String summary,
            String replyDraft
    ) {
    }
}
