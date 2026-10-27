package com.evallab.websearch;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 本地搜索索引：固定的 8 篇文档，按关键词命中数打分。
 *
 * <p>故意做成确定性的：评测要可复现，真实搜索 API 的结果每天都在变，
 * 拿它做回归基线会让门禁一直误报。
 */
@Component
public class LocalSearchIndex implements SearchProvider {

    private static final List<Doc> DOCS = List.of(
            new Doc("Spring Boot 3.5 发布说明", "spring boot releasing",
                    "https://docs.spring.io/spring-boot/3.5/release-notes",
                    "Spring Boot 3.5 于 2025 年 5 月发布，基于 Spring Framework 6.2，"
                            + "支持 JDK 21 与虚拟线程，新增结构化日志与更快的启动时间。"),
            new Doc("JDK 21 虚拟线程指南", "java jdk 21 virtual threads",
                    "https://openjdk.org/jeps/444",
                    "JDK 21 正式推出虚拟线程（JEP 444），以极低的线程成本支持高并发，"
                            + "适合 IO 密集型服务，但不适合长时间 CPU 计算。"),
            new Doc("MySQL 8.0 索引优化", "mysql index optimization",
                    "https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html",
                    "MySQL 8.0 支持降序索引、不可见索引与直方图统计，"
                            + "合理使用覆盖索引可以避免回表，显著降低查询延迟。"),
            new Doc("LLM-as-Judge 评估方法", "llm judge evaluation",
                    "https://arxiv.org/abs/2306.05685",
                    "用强模型给弱模型的输出打分是常见的评估手段。温度固定为 0 可以提高可复现性，"
                            + "同时需要用人工抽检校准，避免评分漂移。"),
            new Doc("Agent 失败归因分层", "agent failure attribution",
                    "https://example.com/agent-failure-layers",
                    "把 Agent 失败拆成输入理解、规划、工具调用、输出生成与环境问题五层，"
                            + "每层用 Trace 证据区分，可以避免「所有失败都归到模型不行」。"),
            new Doc("MyBatis-Plus 分页与雪花 ID", "mybatis plus snowflake id",
                    "https://baomidou.com/guides/snowflake",
                    "雪花 ID 是 64 位长整型，直接返回前端会超出 JS 安全整数范围，"
                            + "需要序列化为字符串；分页查询要显式传入页码与每页条数。"),
            new Doc("Docker Compose 服务编排", "docker compose",
                    "https://docs.docker.com/compose/",
                    "Compose 用一份 YAML 描述多容器应用，depends_on 只能保证启动顺序，"
                            + "要等服务可用还需配合 healthcheck 与 restart 策略。"),
            new Doc("Vue 3 组合式 API", "vue 3 composition api",
                    "https://vuejs.org/guide/extras/composition-api-faq.html",
                    "组合式 API 把状态与逻辑按功能聚合，配合 setup 语法糖可以让组件更易读，"
                            + "跨组件复用逻辑用组合函数而不是 mixin。")
    );

    @Override
    public List<SearchHit> search(String query, int limit) {
        List<String> terms = terms(query);
        List<Scored> scored = new ArrayList<>();
        for (Doc doc : DOCS) {
            double score = score(doc, terms);
            if (score > 0) {
                scored.add(new Scored(doc, score));
            }
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing(s -> s.doc().title))
                .limit(Math.max(1, limit))
                .map(s -> new SearchHit(s.doc().title(), snippet(s.doc().body()), s.doc().url(),
                        s.doc().body(), s.score()))
                .toList();
    }

    @Override
    public String fetch(SearchHit hit) {
        return hit.body();
    }

    private static double score(Doc doc, List<String> terms) {
        String haystack = (doc.title() + " " + doc.keywords() + " " + doc.body()).toLowerCase(Locale.ROOT);
        double score = 0;
        for (String term : terms) {
            if (haystack.contains(term)) {
                score += 1;
            }
        }
        return score;
    }

    /** 中文按二字切分，英文按空白切分；够用即可，不引入分词器。 */
    private static List<String> terms(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>();
        for (String raw : query.toLowerCase(Locale.ROOT).split("[\\s,，。？?！!、；;：:]+")) {
            if (raw.isBlank()) {
                continue;
            }
            if (raw.matches("[\\x00-\\x7f]+")) {
                terms.add(raw);
                continue;
            }
            for (int i = 0; i + 2 <= raw.length(); i++) {
                terms.add(raw.substring(i, i + 2));
            }
        }
        return terms;
    }

    /** 摘要取正文前 60 字，够检索结果列表展示。 */
    private static String snippet(String body) {
        return body.length() <= 60 ? body : body.substring(0, 60) + "…";
    }

    private record Doc(String title, String keywords, String url, String body) {
    }

    private record Scored(Doc doc, double score) {
    }
}
