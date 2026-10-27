package com.evallab.agent.fault;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.TraceReport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 平台拉取入口，契约见 docs/TRACE_CONTRACT.md。 */
@RestController
public class AgentController {

    private final SimulatedWorkflow workflow;
    private final AgentProperties properties;
    private final AgentDegradation degradation;

    public AgentController(SimulatedWorkflow workflow, AgentProperties properties,
                           AgentDegradation degradation) {
        this.workflow = workflow;
        this.properties = properties;
        this.degradation = degradation;
    }

    @PostMapping("/execute")
    public TraceReport execute(@RequestBody ExecuteRequest request) {
        boolean degraded = degradation.applies(request);
        // 退化耗时也记进首个 span，保持 trace 内部时间轴与墙钟一致
        long extraMs = degraded ? degradation.delayMs() : 0;
        TraceReport report = workflow.run(request, properties.timeoutDelay(), extraMs);
        return degraded ? degradation.apply(report) : report;
    }
}
