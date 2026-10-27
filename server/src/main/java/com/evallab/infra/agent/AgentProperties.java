package com.evallab.infra.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

/**
 * 被测 Agent 配置（见 docs/设计决策.md D1）。
 *
 * <p>v1 不做注册表与 UI：外部 Agent 只需在此加一项配置。
 *
 * @param registry key 为 agent_name，value 为地址与超时
 */
@ConfigurationProperties(prefix = "evallab.agent")
public record AgentProperties(Map<String, Target> registry) {

    public AgentProperties {
        registry = registry == null ? Map.of() : Map.copyOf(registry);
    }

    /**
     * @param baseUrl 形如 http://localhost:8081
     * @param timeout 单次调用超时，需大于 Agent 侧故障注入的阻塞时长才能拿到 TIMEOUT 结果
     */
    public record Target(String baseUrl, @DefaultValue("60s") Duration timeout) {
    }
}
