package com.evallab.infra.seed;

import com.evallab.contract.trace.FaultInjection;
import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCaseDraft;
import com.evallab.core.testcase.TestCaseGroup;
import com.evallab.core.testcase.TestCaseQuery;
import com.evallab.core.testcase.TestCaseService;
import com.evallab.contract.trace.FaultType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 演示用例：5 个正常 + 5 个故障注入，与故障注入 Agent 的确定性行为一一对应。
 *
 * <p>默认只在 dev profile 开启；表非空时跳过，避免覆盖手工数据。
 */
@Component
@ConditionalOnProperty(prefix = "evallab.demo", name = "seed-enabled", havingValue = "true")
public class DemoCaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCaseSeeder.class);

    private static final String AGENT_INPUT = "查询 Spring Boot 3.5 发布日期并总结要点";
    private static final List<String> NORMAL_TOOLS = List.of("web_search", "extract", "summarize");

    private final TestCaseService testCaseService;

    public DemoCaseSeeder(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!testCaseService.query(new TestCaseQuery(null, null, false)).isEmpty()) {
            log.info("已有用例，跳过演示数据初始化");
            return;
        }
        TestCaseGroup group = testCaseService.createGroup("demo");
        int created = 0;
        for (TestCaseDraft draft : cases(group.groupId())) {
            testCaseService.create(draft);
            created++;
        }
        log.info("已初始化 {} 个演示用例", created);
    }

    private List<TestCaseDraft> cases(Long groupId) {
        String exactOutput = "{\"answer\": \"" + AGENT_INPUT + "\", \"steps\": 3}";
        String schema = """
                {"type":"object","required":["answer","steps"],
                 "properties":{"answer":{"type":"string","minLength":1},"steps":{"type":"integer","minimum":3}}}
                """;
        return List.of(
                // ---- 正常用例：应全部 PASS ----
                draft(groupId, "normal-exact", AGENT_INPUT, MatchType.EXACT, exactOutput, null, null),
                draft(groupId, "normal-regex", AGENT_INPUT, MatchType.REGEX,
                        "\\{\"answer\": \".+\", \"steps\": 3}", null, null),
                draft(groupId, "normal-json-schema", AGENT_INPUT, MatchType.JSON_SCHEMA, schema, null, null),
                draft(groupId, "normal-tools-only", AGENT_INPUT, MatchType.NONE, null, NORMAL_TOOLS, null),
                draft(groupId, "normal-both", AGENT_INPUT, MatchType.EXACT, exactOutput, NORMAL_TOOLS, null),

                // ---- 故障注入用例：应全部 FAIL ----
                draft(groupId, "fault-tool-error", AGENT_INPUT, MatchType.NONE, null, NORMAL_TOOLS,
                        new FaultInjection(FaultType.TOOL_ERROR, "0")),
                draft(groupId, "fault-step-missing", AGENT_INPUT, MatchType.NONE, null, NORMAL_TOOLS,
                        new FaultInjection(FaultType.STEP_MISSING, null)),
                draft(groupId, "fault-output-format", AGENT_INPUT, MatchType.JSON_SCHEMA, schema, null,
                        new FaultInjection(FaultType.OUTPUT_FORMAT_ERROR, null)),
                draft(groupId, "fault-timeout", AGENT_INPUT, MatchType.NONE, null, NORMAL_TOOLS,
                        new FaultInjection(FaultType.TIMEOUT, null)),
                draft(groupId, "fault-input-misunderstand", AGENT_INPUT, MatchType.EXACT, exactOutput, NORMAL_TOOLS,
                        new FaultInjection(FaultType.INPUT_MISUNDERSTAND, null))
        );
    }

    private static TestCaseDraft draft(Long groupId, String name, String input, MatchType matchType,
                                       String expectedOutput, List<String> expectedTools,
                                       FaultInjection fault) {
        return new TestCaseDraft(groupId, name, input, matchType, expectedOutput, expectedTools,
                fault, List.of("demo"), true);
    }
}
