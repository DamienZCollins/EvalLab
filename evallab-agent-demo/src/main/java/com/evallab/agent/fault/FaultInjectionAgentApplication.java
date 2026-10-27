package com.evallab.agent.fault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 扫描根放宽到 {@code com.evallab.agent}：{@code fault}（工作流与故障）与 {@code guard}（输入侧防御）
 * 是两个独立关注点，启动类放在 {@code fault} 下会让 {@code guard} 的组件扫描不到。
 */
@SpringBootApplication(scanBasePackages = "com.evallab.agent")
@ConfigurationPropertiesScan
public class FaultInjectionAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaultInjectionAgentApplication.class, args);
    }
}
