package com.evallab.infra.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Judge 用的 LLM 配置，兼容任意 OpenAI 协议端点（OpenAI、DeepSeek、Ollama 等）。
 *
 * <p>{@code base-url} 为空表示不启用 Judge：评测照常跑，只是没有参考型评分，
 * 这样本地无模型时不会阻塞阶段 2 的确定性评分链路。
 *
 * @param baseUrl     OpenAI 兼容端点根地址，如 https://api.openai.com
 * @param apiKey      密钥；本地端点（Ollama 等）可留空，但 Spring AI 要求非空，故缺省为占位符
 * @param model       模型名
 * @param temperature 采样温度，Judge 取 0 保证可复现
 * @param timeout     单次调用超时
 */
@ConfigurationProperties(prefix = "evallab.llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        @DefaultValue("gpt-4o-mini") String model,
        @DefaultValue("0.0") Double temperature,
        @DefaultValue("60s") Duration timeout
) {

    /** 未配置 base_url 时视为关闭；空串按未配置处理。 */
    public boolean enabled() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    public LlmProperties {
        if (apiKey == null || apiKey.isBlank()) {
            // 本地 OpenAI 兼容端点通常不校验 key，但 Spring AI 的 Builder 强制要求非空
            apiKey = "not-required";
        }
    }
}
