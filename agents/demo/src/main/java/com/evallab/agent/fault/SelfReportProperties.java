package com.evallab.agent.fault;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 启动后自动把一组示例 Trace 上报到平台，用于阶段 1 的链路验证。
 *
 * @param enabled     是否开启
 * @param platformUrl 平台地址，如 http://localhost:8080
 */
@ConfigurationProperties(prefix = "evallab.self-report")
public record SelfReportProperties(boolean enabled, String platformUrl) {
}
