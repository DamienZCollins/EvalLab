package com.evallab.websearch;

import com.evallab.contract.trace.EventType;
import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanEvent;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 搜索 → 抓取 → 总结 的三步工具链。
 *
 * <p>与故障注入 Agent 不同，这里每一步的耗时和 token 都按输入长度估算，
 * 同一输入重复执行结果完全一致，适合做回归基线。
 *
 * <p>输出固定为 JSON：{@code {"answer": "<首条结果标题>", "sources": <抓取篇数>, "steps": <工具调用数>}}；
 * 无命中时 answer 为「未找到相关结果」，steps 记实际发生的调用数，便于用例按工具序列判定。
 */
@Component
public class WebSearchWorkflow {

    static final String TOOL_SEARCH = "web_search";
    static final String TOOL_FETCH = "fetch_page";

    private final SearchProvider searchProvider;
    private final AgentProperties properties;

    public WebSearchWorkflow(SearchProvider searchProvider, AgentProperties properties) {
        this.searchProvider = searchProvider;
        this.properties = properties;
    }

    public TraceReport run(ExecuteRequest request) {
        String query = request.input() == null ? "" : request.input().trim();
        Instant started = Instant.now();
        List<SpanDto> spans = new ArrayList<>();
        List<String> tools = new ArrayList<>();

        String planId = id();
        Instant planEnd = addSpan(spans, planId, null, SpanType.LLM_CALL, null,
                query, "计划：搜索 → 抓取 → 总结", started, 40 + estimate(query, "计划"));

        List<SearchProvider.SearchHit> hits = searchProvider.search(query, properties.topK());
        String searchId = id();
        Instant searchEnd = addSpan(spans, searchId, planId, SpanType.TOOL_CALL, TOOL_SEARCH,
                "{\"query\": \"" + query + "\", \"limit\": " + properties.topK() + "}",
                "{\"hits\": " + hits.size() + "}", planEnd, 60 + hits.size() * 5L);
        tools.add(TOOL_SEARCH);

        String parent = searchId;
        Instant cursor = searchEnd;
        int fetched = 0;
        for (SearchProvider.SearchHit hit : hits) {
            String body = searchProvider.fetch(hit);
            String fetchId = id();
            Instant end = addSpan(spans, fetchId, searchId, SpanType.TOOL_CALL, TOOL_FETCH,
                    "{\"url\": \"" + hit.url() + "\"}",
                    "{\"length\": " + body.length() + "}", cursor, 80 + body.length() / 20L);
            tools.add(TOOL_FETCH);
            fetched++;
            // 总结挂在最后一次抓取下，形成「搜索 → 抓取 → 总结」的链式层级
            parent = fetchId;
            cursor = end;
        }

        String answer = hits.isEmpty() ? "未找到相关结果" : hits.get(0).title();
        String summary = hits.isEmpty()
                ? "索引中没有与该查询匹配的文档，请换个说法。"
                : "首条结果为《" + hits.get(0).title() + "》，抓取 " + fetched + " 篇正文后归纳。";
        String output = "{\"answer\": \"" + answer + "\", \"sources\": " + fetched
                + ", \"steps\": " + tools.size() + "}";

        String summarizeId = id();
        Instant summarizeEnd = addSpan(spans, summarizeId, parent, SpanType.LLM_CALL, null,
                summary, output, cursor, 120 + estimate(summary, output));

        long durationMs = Duration.between(spans.get(0).startTime(), summarizeEnd).toMillis();
        int tokens = spans.stream().mapToInt(s -> s.tokens() == null ? 0 : s.tokens()).sum();
        return new TraceReport(UUID.randomUUID().toString(), properties.name(), TraceStatus.SUCCESS,
                output, tokens, durationMs, spans);
    }

    private Instant addSpan(List<SpanDto> spans, String spanId, String parentId, SpanType type,
                            String toolName, String input, String output, Instant start, long durationMs) {
        Instant end = start.plusMillis(durationMs);
        spans.add(new SpanDto(spanId, parentId, type, toolName, input, output, null,
                estimate(input, output), durationMs, start, end,
                List.of(new SpanEvent(EventType.LOG,
                        toolName == null ? "推理完成" : toolName + " 完成", end))));
        return end;
    }

    private static int estimate(String input, String output) {
        int chars = (input == null ? 0 : input.length()) + (output == null ? 0 : output.length());
        return Math.max(1, chars / 4);
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }
}
