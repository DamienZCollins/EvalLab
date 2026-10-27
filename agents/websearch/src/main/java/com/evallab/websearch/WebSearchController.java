package com.evallab.websearch;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.TraceReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台拉取入口，契约见 docs/TRACE_CONTRACT.md。
 */
@RestController
public class WebSearchController {

    private final WebSearchWorkflow workflow;
    private final AgentProperties properties;

    public WebSearchController(WebSearchWorkflow workflow, AgentProperties properties) {
        this.workflow = workflow;
        this.properties = properties;
    }

    @PostMapping("/execute")
    public TraceReport execute(@RequestBody ExecuteRequest request) {
        return workflow.run(request);
    }

    /** 平台配置 agent 注册表时需要知道对方的 agent_name，此处直接暴露。 */
    @GetMapping("/agent")
    public AgentInfo describe() {
        return new AgentInfo(properties.name());
    }

    public record AgentInfo(String agentName) {
    }
}
