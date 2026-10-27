package com.evallab.core.baseline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 基线持久化端口。 */
public interface BaselineStore {

    Baseline create(String name, Long taskId, String agentName, int totalCases,
                    BigDecimal passRate, BigDecimal avgScore, Long p95DurationMs);

    List<Baseline> findAll();

    Optional<Baseline> find(long baselineId);

    /** 同一任务只允许一条基线（表上有唯一键）。 */
    Optional<Baseline> findByTask(long taskId);
}
