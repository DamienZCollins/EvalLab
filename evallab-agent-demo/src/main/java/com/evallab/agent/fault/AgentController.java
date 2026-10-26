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

    public AgentController(SimulatedWorkflow workflow, AgentProperties properties) {
        this.workflow = workflow;
        this.properties = properties;
    }

    @PostMapping("/execute")
    public TraceReport execute(@RequestBody ExecuteRequest request) {
        return workflow.run(request, properties.timeoutDelay());
    }
}
