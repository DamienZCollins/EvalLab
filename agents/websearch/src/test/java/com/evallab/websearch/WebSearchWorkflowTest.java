package com.evallab.websearch;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchWorkflowTest {

    private final WebSearchWorkflow workflow =
            new WebSearchWorkflow(new LocalSearchIndex(), new AgentProperties("web-search-agent", 1));

    @Test
    void producesNestedSpanTreeWithThreeSteps() {
        TraceReport trace = workflow.run(request("Spring Boot 3.5 发布日期"));

        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertEquals("web-search-agent", trace.agentName());
        assertNotNull(trace.traceId());

        List<SpanDto> spans = trace.spans();
        SpanDto plan = spans.get(0);
        SpanDto search = spans.get(1);
        SpanDto fetch = spans.get(2);
        SpanDto summarize = spans.get(3);

        // 规划 → 搜索 → 抓取 → 总结，四层链式嵌套
        assertEquals(null, plan.parentSpanId());
        assertEquals(SpanType.LLM_CALL, plan.spanType());
        assertEquals(plan.spanId(), search.parentSpanId());
        assertEquals("web_search", search.toolName());
        assertEquals(search.spanId(), fetch.parentSpanId());
        assertEquals("fetch_page", fetch.toolName());
        assertEquals(fetch.spanId(), summarize.parentSpanId());

        assertTrue(trace.output().contains("\"steps\": 2"), trace.output());
    }

    @Test
    void fetchesTopKPages() {
        // 该查询同时命中 Spring Boot 与 JDK 21 两篇文档，top-k=2 时抓取两次
        WebSearchWorkflow two = new WebSearchWorkflow(new LocalSearchIndex(),
                new AgentProperties("web-search-agent", 2));
        TraceReport trace = two.run(request("Spring Boot JDK 21"));
        long fetches = trace.spans().stream().filter(s -> "fetch_page".equals(s.toolName())).count();
        assertEquals(2, fetches);
        assertTrue(trace.output().contains("\"steps\": 3"), trace.output());
    }

    @Test
    void reportsNoResultWhenNothingMatches() {
        TraceReport trace = workflow.run(request("量子纠缠拓扑绝缘体"));

        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertTrue(trace.output().contains("未找到相关结果"), trace.output());
        assertTrue(trace.output().contains("\"steps\": 1"), trace.output());
    }

    @Test
    void isDeterministicForSameInput() {
        TraceReport first = workflow.run(request("MySQL 索引优化"));
        TraceReport second = workflow.run(request("MySQL 索引优化"));

        assertEquals(first.output(), second.output());
        assertEquals(first.spans().size(), second.spans().size());
        assertEquals(first.spans().get(1).output(), second.spans().get(1).output());
    }

    @Test
    void everySpanHasEvents() {
        TraceReport trace = workflow.run(request("Vue 3 组合式 API"));
        assertTrue(trace.spans().stream().allMatch(s -> s.events() != null && !s.events().isEmpty()));
    }

    private static ExecuteRequest request(String input) {
        return new ExecuteRequest("task", "case", input, null);
    }
}
