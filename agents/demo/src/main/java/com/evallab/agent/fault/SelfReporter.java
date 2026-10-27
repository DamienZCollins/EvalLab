package com.evallab.agent.fault;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.FaultInjection;
import com.evallab.contract.trace.FaultType;
import com.evallab.contract.trace.TraceReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 启动后自动跑一个正常 Trace 和 5 个故障 Trace 并上报，用于阶段 1 的链路验证。
 *
 * <p>失败只记录日志，不阻止 Agent 启动——平台没起来时 Agent 仍应可独立运行。
 */
@Component
@ConditionalOnProperty(prefix = "evallab.self-report", name = "enabled", havingValue = "true")
public class SelfReporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SelfReporter.class);

    private static final String NORMAL_INPUT = "查询 Spring Boot 3.5 的发布日期并总结要点";

    private final SimulatedWorkflow workflow;
    private final SelfReportProperties properties;

    public SelfReporter(SimulatedWorkflow workflow, SelfReportProperties properties) {
        this.workflow = workflow;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        RestClient client = RestClient.create(properties.platformUrl());
        report(client, workflow.run(new ExecuteRequest(null, "self-check-normal", NORMAL_INPUT, null),
                Duration.ZERO));
        for (FaultType type : FaultType.values()) {
            ExecuteRequest request = new ExecuteRequest(null, "self-check-" + type.name(), NORMAL_INPUT,
                    new FaultInjection(type, null));
            report(client, workflow.run(request, Duration.ZERO));
        }
    }

    private void report(RestClient client, TraceReport trace) {
        try {
            client.post()
                    .uri("/api/trace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(trace)
                    .retrieve()
                    .toBodilessEntity();
            log.info("自检上报成功 trace_id={} status={}", trace.traceId(), trace.status());
        } catch (Exception e) {
            log.warn("自检上报失败，平台可能未启动: {}", e.getMessage());
        }
    }
}
