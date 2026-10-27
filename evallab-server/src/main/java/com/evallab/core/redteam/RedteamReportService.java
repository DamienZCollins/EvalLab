package com.evallab.core.redteam;

import com.evallab.core.eval.EvalResult;
import com.evallab.core.eval.EvalTask;
import com.evallab.core.eval.EvalTaskNotFoundException;
import com.evallab.core.eval.EvalTaskStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 红队评测报告：拦截率 = 防御成功数 / 总攻击数。
 *
 * <p>只统计任务结果中属于红队用例的那些结果。平台侧「被攻破」就是该用例 FAIL，
 * 判定来自守卫正则的确定性评分，不含 Judge（见设计决策 D5）。
 */
@Service
public class RedteamReportService {

    private final EvalTaskStore taskStore;
    private final RedteamCaseStore redteamStore;

    public RedteamReportService(EvalTaskStore taskStore, RedteamCaseStore redteamStore) {
        this.taskStore = taskStore;
        this.redteamStore = redteamStore;
    }

    public RedteamReport report(long taskId) {
        EvalTask task = taskStore.findTask(taskId)
                .orElseThrow(() -> new EvalTaskNotFoundException(taskId));
        Map<Long, RedteamCase> cases = redteamStore.find(null).stream()
                .collect(Collectors.toMap(RedteamCase::testcaseId, Function.identity()));

        List<Attack> attacks = taskStore.findResults(taskId).stream()
                .filter(r -> cases.containsKey(r.testcaseId()))
                .map(r -> attack(r, cases.get(r.testcaseId())))
                .toList();

        int intercepted = (int) attacks.stream().filter(Attack::intercepted).count();
        int total = attacks.size();
        BigDecimal rate = total == 0 ? null
                : BigDecimal.valueOf(intercepted).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

        return new RedteamReport(task.taskId(), task.agentName(), task.status().name(), total,
                intercepted, total - intercepted, rate, byType(attacks), attacks);
    }

    private static Attack attack(EvalResult result, RedteamCase redteamCase) {
        return new Attack(redteamCase.redteamCaseId(), redteamCase.name(), redteamCase.attackType(),
                result.passed(), result.failureReason(), result.traceId());
    }

    private static Map<RedteamAttackType, TypeStat> byType(List<Attack> attacks) {
        Map<RedteamAttackType, List<Attack>> grouped = new LinkedHashMap<>();
        for (RedteamAttackType type : RedteamAttackType.values()) {
            grouped.put(type, new ArrayList<>());
        }
        attacks.forEach(a -> grouped.computeIfAbsent(a.attackType(), k -> new ArrayList<>()).add(a));
        Map<RedteamAttackType, TypeStat> stats = new EnumMap<>(RedteamAttackType.class);
        grouped.forEach((type, list) -> {
            if (list.isEmpty()) {
                return;
            }
            int intercepted = (int) list.stream().filter(Attack::intercepted).count();
            BigDecimal rate = BigDecimal.valueOf(intercepted)
                    .divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP);
            stats.put(type, new TypeStat(list.size(), intercepted, list.size() - intercepted, rate));
        });
        return stats;
    }

    /**
     * @param interceptionRate 拦截率（0–1）；任务中没有红队用例时为 null
     */
    public record RedteamReport(
            Long taskId,
            String agentName,
            String status,
            int totalAttacks,
            int interceptedAttacks,
            int breachedAttacks,
            BigDecimal interceptionRate,
            Map<RedteamAttackType, TypeStat> byType,
            List<Attack> attacks
    ) {
    }

    public record TypeStat(int totalAttacks, int interceptedAttacks, int breachedAttacks,
                           BigDecimal interceptionRate) {
    }

    /** @param intercepted true 表示防御成功（平台侧 PASS） */
    public record Attack(Long redteamCaseId, String name, RedteamAttackType attackType,
                         boolean intercepted, String failureReason, String traceId) {
    }
}
