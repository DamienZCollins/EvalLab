package com.evallab.core.scorer;

import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCase;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 规则评分器：按 match_type 分派，二值 1.0 / 0.0（见 docs/设计决策.md D3）。
 */
@Component
public class RuleScorer {

    private static final BigDecimal PASS = BigDecimal.ONE;
    private static final BigDecimal FAIL = BigDecimal.ZERO;

    private final ObjectMapper objectMapper;

    public RuleScorer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @return 该用例对应的评分记录；match_type=NONE 时返回 null
     */
    public ScoreRecord score(TestCase testCase, AgentOutput agentOutput) {
        if (testCase.matchType() == null || testCase.matchType() == MatchType.NONE) {
            return null;
        }
        String actual = agentOutput.output() == null ? "" : agentOutput.output();
        String expected = testCase.expectedOutput();
        return switch (testCase.matchType()) {
            case EXACT -> exact(actual, expected);
            case REGEX -> regex(actual, expected);
            case JSON_SCHEMA -> jsonSchema(actual, expected);
            case NONE -> null;
        };
    }

    private ScoreRecord exact(String actual, String expected) {
        boolean ok = actual.trim().equals(expected == null ? "" : expected.trim());
        return judging("exact_match", ok, ok ? "输出与期望完全一致" : "输出与期望不一致");
    }

    private ScoreRecord regex(String actual, String expected) {
        boolean ok;
        try {
            ok = Pattern.matches(expected, actual);
        } catch (RuntimeException e) {
            return judging("regex", false, "正则执行失败: " + e.getMessage());
        }
        return judging("regex", ok, ok ? "输出匹配正则" : "输出不匹配正则");
    }

    private ScoreRecord jsonSchema(String actual, String expected) {
        JsonNode output;
        try {
            output = objectMapper.readTree(actual);
        } catch (JsonProcessingException e) {
            return judging("json_schema", false, "输出不是合法 JSON");
        }
        if (output == null) {
            return judging("json_schema", false, "输出不是合法 JSON");
        }
        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            JsonSchema schema = factory.getSchema(expected);
            Set<ValidationMessage> errors = schema.validate(output);
            if (errors.isEmpty()) {
                return judging("json_schema", true, "输出符合 Schema");
            }
            String detail = errors.stream().map(ValidationMessage::getMessage).limit(3)
                    .reduce((a, b) -> a + "; " + b).orElse("");
            return judging("json_schema", false, "输出不符合 Schema: " + detail);
        } catch (RuntimeException e) {
            return judging("json_schema", false, "Schema 无法解析: " + e.getMessage());
        }
    }

    private static ScoreRecord judging(String name, boolean ok, String reason) {
        return ScoreRecord.judging(ScorerType.RULE, name, ok ? PASS : FAIL, PASS, reason);
    }
}
