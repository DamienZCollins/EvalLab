package com.evallab.agent.fault;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param name         上报到平台的 agent_name
 * @param timeoutDelay TIMEOUT 故障下阻塞多久再返回
 */
@ConfigurationProperties(prefix = "evallab.agent")
public record AgentProperties(String name, Duration timeoutDelay) {
}
