package com.evallab.api.redteam;

import com.evallab.api.common.IdJson;
import com.evallab.core.redteam.RedteamAttackType;
import com.evallab.core.redteam.RedteamReportService;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 红队评测报告。
 *
 * @param interceptionRate 拦截率（0–1）；任务里没有红队用例时为 null
 */
@Schema(description = "红队评测报告")
public record RedteamReportResponse(
        @JsonSerialize(using = IdJson.class) Long taskId,
        String agentName,
        String status,
        int totalAttacks,
        int interceptedAttacks,
        int breachedAttacks,
        BigDecimal interceptionRate,
        Map<RedteamAttackType, TypeStatResponse> byType,
        List<AttackResponse> attacks
) {

    public static RedteamReportResponse of(RedteamReportService.RedteamReport report) {
        return new RedteamReportResponse(report.taskId(), report.agentName(), report.status(),
                report.totalAttacks(), report.interceptedAttacks(), report.breachedAttacks(),
                report.interceptionRate(),
                report.byType().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new TypeStatResponse(e.getValue().totalAttacks(),
                                e.getValue().interceptedAttacks(), e.getValue().breachedAttacks(),
                                e.getValue().interceptionRate()))),
                report.attacks().stream().map(AttackResponse::of).toList());
    }

    @Schema(description = "按攻击类型的拦截统计")
    public record TypeStatResponse(int totalAttacks, int interceptedAttacks, int breachedAttacks,
                                   BigDecimal interceptionRate) {
    }

    @Schema(description = "单条攻击的结果")
    public record AttackResponse(
            @JsonSerialize(using = IdJson.class) Long redteamCaseId,
            String name,
            RedteamAttackType attackType,
            @Schema(description = "true 表示防御成功（平台侧 PASS）") boolean intercepted,
            String failureReason,
            String traceId
    ) {

        static AttackResponse of(RedteamReportService.Attack attack) {
            return new AttackResponse(attack.redteamCaseId(), attack.name(), attack.attackType(),
                    attack.intercepted(), attack.failureReason(), attack.traceId());
        }
    }
}
