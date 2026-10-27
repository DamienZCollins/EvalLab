package com.evallab.infra.llm;

import com.evallab.core.scorer.JudgeModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 用 Spring AI ChatClient 实现 Judge，Prompt 参考 DeepEval G-Eval 的三段式。
 *
 * <p>要求模型返回严格 JSON；模型偶尔会包一层 ```json 代码块，这里一并剥掉。
 */
@Component
@ConditionalOnBean(name = "judgeChatClient")
public class OpenAiJudgeModel implements JudgeModel {

    private static final Logger log = LoggerFactory.getLogger(OpenAiJudgeModel.class);

    private static final String SYSTEM_PROMPT = """
            你是一名严格的 Agent 输出评测专家。请从相关性、正确性、完整性三个维度，
            对比「期望输出」评价「实际输出」的质量，给出 0 到 1 之间的分数：
            1.0 表示与期望等价且信息完整，0.0 表示完全错误或答非所问。

            只返回 JSON，不要任何解释性文字或 Markdown 代码块：
            {"score": 0.85, "reason": "一句话说明扣分点"}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAiJudgeModel(ChatClient judgeChatClient, ObjectMapper objectMapper) {
        this.chatClient = judgeChatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public JudgeVerdict judge(String input, String expectedOutput, String actualOutput) {
        String user = """
                任务输入：
                %s

                期望输出：
                %s

                实际输出：
                %s
                """.formatted(oneLine(input), oneLine(expectedOutput), oneLine(actualOutput));

        String content;
        try {
            content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new JudgeUnavailableException("Judge 调用失败: " + e.getMessage(), e);
        }
        return parse(content);
    }

    private JudgeVerdict parse(String content) {
        if (content == null || content.isBlank()) {
            throw new JudgeUnavailableException("Judge 返回空响应", null);
        }
        String json = stripCodeFence(content.trim());
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode scoreNode = node.get("score");
            if (scoreNode == null || !scoreNode.isNumber()) {
                throw new JudgeUnavailableException("Judge 响应缺少 score: " + abbreviate(json), null);
            }
            String reason = node.hasNonNull("reason") ? node.get("reason").asText() : "";
            return new JudgeVerdict(scoreNode.decimalValue(), reason);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new JudgeUnavailableException("Judge 响应不是合法 JSON: " + abbreviate(json), e);
        }
    }

    /** 去掉 ```json ... ``` 包裹；模型偶尔无视「不要代码块」的约束。 */
    private static String stripCodeFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        if (firstNewline < 0) {
            return text;
        }
        String body = text.substring(firstNewline + 1);
        int closing = body.lastIndexOf("```");
        return closing < 0 ? body : body.substring(0, closing).trim();
    }

    /** Prompt 里保留换行会让多行输出难以对齐，压缩为单行但保留全部内容。 */
    private static String oneLine(String text) {
        return text == null ? "" : text.replace("\r", "").replace("\n", "\\n");
    }

    private static String abbreviate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
