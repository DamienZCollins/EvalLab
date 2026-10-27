package com.evallab.core.testcase;

import com.evallab.contract.trace.FaultType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 用例校验。重点是「按 D3 声明了期望，但评分器无法解释」的情况，这类错误必须在创建时就拦下。
 */
public final class TestCaseValidator {

    private static final int MAX_NAME = 128;

    private TestCaseValidator() {
    }

    public static List<String> validate(TestCaseDraft draft) {
        List<String> violations = new ArrayList<>();
        if (draft == null) {
            return List.of("请求体为空");
        }
        if (!hasText(draft.name())) {
            violations.add("name 不能为空");
        } else if (draft.name().length() > MAX_NAME) {
            violations.add("name 超过最大长度 " + MAX_NAME);
        }
        if (!hasText(draft.input())) {
            violations.add("input 不能为空");
        }
        if (draft.matchType() == null) {
            violations.add("match_type 不能为空");
            return violations;
        }
        validateRule(draft, violations);
        validateTools(draft, violations);
        validateFault(draft, violations);
        return violations;
    }

    private static void validateRule(TestCaseDraft draft, List<String> violations) {
        MatchType type = draft.matchType();
        if (type == MatchType.NONE) {
            if (hasText(draft.expectedOutput())) {
                violations.add("match_type=NONE 时 expected_output 必须为空");
            }
            return;
        }
        if (!hasText(draft.expectedOutput())) {
            violations.add("match_type=" + type + " 时 expected_output 不能为空");
            return;
        }
        switch (type) {
            case REGEX -> {
                try {
                    Pattern.compile(draft.expectedOutput());
                } catch (PatternSyntaxException e) {
                    violations.add("expected_output 不是合法正则: " + e.getDescription());
                }
            }
            case JSON_SCHEMA -> {
                // Schema 本身是否可用由评分器在运行时校验，这里只保证非空
            }
            default -> {
            }
        }
    }

    private static void validateTools(TestCaseDraft draft, List<String> violations) {
        if (draft.expectedTools() == null) {
            return;
        }
        if (draft.expectedTools().stream().anyMatch(t -> !hasText(t))) {
            violations.add("expected_tools 不能包含空工具名");
        }
    }

    private static void validateFault(TestCaseDraft draft, List<String> violations) {
        if (draft.faultInjection() == null) {
            return;
        }
        FaultType type = draft.faultInjection().type();
        if (type == null) {
            violations.add("fault_injection.type 不能为空");
            return;
        }
        String target = draft.faultInjection().targetSpan();
        if (target != null && !target.isBlank()) {
            try {
                Integer.parseInt(target.trim());
            } catch (NumberFormatException e) {
                violations.add("fault_injection.target_span 必须是整数序号: " + target);
            }
        }
    }

    /** 按 D2：至少启用一个判定型评分器，否则用例无法判定 PASS/FAIL。 */
    public static List<String> validateJudgingScorer(MatchType matchType, List<String> expectedTools) {
        boolean rule = matchType != null && matchType != MatchType.NONE;
        boolean tools = expectedTools != null && !expectedTools.isEmpty();
        if (rule || tools) {
            return List.of();
        }
        return List.of("用例必须至少声明一种判定依据：expected_output（match_type 非 NONE）或 expected_tools");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
