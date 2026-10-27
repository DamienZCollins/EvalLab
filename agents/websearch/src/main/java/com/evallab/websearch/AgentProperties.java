package com.evallab.websearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param name  上报到平台的 agent_name
 * @param topK  每次搜索取前几条结果，也决定抓取次数
 */
@ConfigurationProperties(prefix = "evallab.websearch")
public record AgentProperties(String name, Integer topK) {

    public AgentProperties {
        name = name == null || name.isBlank() ? "web-search-agent" : name;
        topK = topK == null || topK < 1 ? 1 : Math.min(topK, 3);
    }
}
