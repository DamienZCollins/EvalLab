package com.evallab.infra.seed;

import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCaseDraft;
import com.evallab.core.testcase.TestCaseGroup;
import com.evallab.core.testcase.TestCaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Web 搜索 Agent 的 20 个演示用例。
 *
 * <p>与 {@link DemoCaseSeeder} 分开：后者整表非空就跳过，已有环境的库里不会再补；
 * 这里只按分组判断，升级后重启也能拿到这组用例。用例输入与 {@code LocalSearchIndex} 的
 * 8 篇文档一一对应，预期答案就是文档标题，因此这组用例对当前实现应当全通过——
 * 若哪天不通过，多半是搜索排序或输出格式被改坏了。
 *
 * <p>覆盖四种判定方式：EXACT 8 个、REGEX 5 个、JSON_SCHEMA 5 个、工具序列 2 个，
 * 以及一个无命中的边界用例。
 */
@Component
@ConditionalOnProperty(prefix = "evallab.demo", name = "websearch-cases-enabled", havingValue = "true")
public class WebSearchCaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebSearchCaseSeeder.class);

    static final String GROUP = "websearch";
    private static final List<String> TOOLS = List.of("web_search", "fetch_page");
    private static final String SCHEMA = """
            {"type":"object","required":["answer","sources","steps"],
             "properties":{"answer":{"type":"string","minLength":1},
                           "sources":{"type":"integer","minimum":1},
                           "steps":{"type":"integer","minimum":2}}}
            """;

    private final TestCaseService testCaseService;

    public WebSearchCaseSeeder(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean exists = testCaseService.groups().stream().anyMatch(g -> GROUP.equals(g.groupName()));
        if (exists) {
            log.info("websearch 分组已存在，跳过 Web 搜索用例初始化");
            return;
        }
        TestCaseGroup group = testCaseService.createGroup(GROUP);
        List<TestCaseDraft> drafts = cases(group.groupId());
        drafts.forEach(testCaseService::create);
        log.info("已初始化 {} 个 Web 搜索用例", drafts.size());
    }

    private List<TestCaseDraft> cases(Long groupId) {
        return List.of(
                // ---- 8 个文档各一条，精确匹配答案 ----
                exact(groupId, "ws-spring-boot", "Spring Boot 3.5 发布日期", "Spring Boot 3.5 发布说明"),
                exact(groupId, "ws-jdk21", "JDK 21 虚拟线程", "JDK 21 虚拟线程指南"),
                exact(groupId, "ws-mysql-index", "MySQL 索引优化", "MySQL 8.0 索引优化"),
                exact(groupId, "ws-llm-judge", "LLM Judge 评估", "LLM-as-Judge 评估方法"),
                exact(groupId, "ws-attribution", "Agent 失败归因", "Agent 失败归因分层"),
                exact(groupId, "ws-snowflake", "MyBatis-Plus 雪花 ID", "MyBatis-Plus 分页与雪花 ID"),
                exact(groupId, "ws-compose", "Docker Compose 编排", "Docker Compose 服务编排"),
                exact(groupId, "ws-vue3", "Vue 3 组合式 API", "Vue 3 组合式 API"),

                // ---- 换匹配方式覆盖其它规则分支 ----
                regex(groupId, "ws-regex-spring", "Spring Boot 3.5 发布日期"),
                regex(groupId, "ws-regex-jdk", "JDK 21 虚拟线程"),
                regex(groupId, "ws-regex-mysql", "MySQL 索引优化"),
                regex(groupId, "ws-regex-compose", "Docker Compose 编排"),
                regex(groupId, "ws-regex-attribution", "Agent 失败归因"),
                schema(groupId, "ws-schema-spring", "Spring Boot 3.5 发布日期"),
                schema(groupId, "ws-schema-judge", "LLM Judge 评估"),
                schema(groupId, "ws-schema-vue3", "Vue 3 组合式 API"),
                schema(groupId, "ws-schema-attribution", "Agent 失败归因"),
                schema(groupId, "ws-schema-snowflake", "MyBatis-Plus 雪花 ID"),

                // ---- 无命中：只有一次搜索，不抓取；同时校验「没结果」时不编造答案 ----
                new TestCaseDraft(groupId, "ws-no-result", "量子纠缠拓扑绝缘体", MatchType.EXACT,
                        "{\"answer\": \"未找到相关结果\", \"sources\": 0, \"steps\": 1}",
                        List.of("web_search"), null, List.of("websearch"), true),

                // ---- 只校验工具序列，不看输出 ----
                new TestCaseDraft(groupId, "ws-tools-only", "MySQL 查询优化", MatchType.NONE,
                        null, TOOLS, null, List.of("websearch"), true)
        );
    }

    private static TestCaseDraft exact(Long groupId, String name, String input, String answer) {
        return new TestCaseDraft(groupId, name, input, MatchType.EXACT,
                output(answer, 1, 2), TOOLS, null, List.of("websearch"), true);
    }

    private static TestCaseDraft regex(Long groupId, String name, String input) {
        return new TestCaseDraft(groupId, name, input, MatchType.REGEX,
                "\\{\"answer\": \".+\", \"sources\": 1, \"steps\": 2}", TOOLS,
                null, List.of("websearch"), true);
    }

    private static TestCaseDraft schema(Long groupId, String name, String input) {
        return new TestCaseDraft(groupId, name, input, MatchType.JSON_SCHEMA, SCHEMA, TOOLS,
                null, List.of("websearch"), true);
    }

    private static String output(String answer, int sources, int steps) {
        return "{\"answer\": \"" + answer + "\", \"sources\": " + sources + ", \"steps\": " + steps + "}";
    }
}
