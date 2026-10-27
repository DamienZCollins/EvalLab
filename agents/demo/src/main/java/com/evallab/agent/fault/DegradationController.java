package com.evallab.agent.fault;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 退化开关的管理接口，供阶段 4 的回归门禁验收与 CI 演示使用。
 *
 * <pre>
 * curl -X POST localhost:8081/admin/degrade \
 *      -H 'Content-Type: application/json' \
 *      -d '{"mode":"FAIL_STATUS","case_ids":["123"]}'
 * </pre>
 */
@RestController
@RequestMapping("/admin/degrade")
public class DegradationController {

    private final AgentDegradation degradation;

    public DegradationController(AgentDegradation degradation) {
        this.degradation = degradation;
    }

    @GetMapping
    public AgentDegradation.State current() {
        return degradation.current();
    }

    @PostMapping
    public AgentDegradation.State configure(@RequestBody DegradationRequest request) {
        degradation.configure(request.mode(), request.delayMs(), request.caseIds());
        return degradation.current();
    }

    @DeleteMapping
    public AgentDegradation.State reset() {
        degradation.reset();
        return degradation.current();
    }

    /**
     * 平台全站 snake_case，但本模块的请求体默认按字段名接收；显式声明两种写法都收，
     * 免得调用方按 snake_case 传参却被静默忽略（case_ids 一直被当成空集合）。
     */
    public record DegradationRequest(
            AgentDegradation.Mode mode,
            @JsonAlias("delay_ms") Long delayMs,
            @JsonAlias("case_ids") List<String> caseIds
    ) {
    }
}
