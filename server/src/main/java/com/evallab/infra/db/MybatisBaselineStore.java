package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.evallab.core.baseline.Baseline;
import com.evallab.core.baseline.BaselineStore;
import com.evallab.infra.db.entity.EvalBaselineEntity;
import com.evallab.infra.db.mapper.EvalBaselineMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisBaselineStore implements BaselineStore {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final EvalBaselineMapper mapper;

    public MybatisBaselineStore(EvalBaselineMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Baseline create(String name, Long taskId, String agentName, int totalCases,
                           BigDecimal passRate, BigDecimal avgScore, Long p95DurationMs) {
        EvalBaselineEntity entity = new EvalBaselineEntity();
        entity.setName(name);
        entity.setTaskId(taskId);
        entity.setAgentName(agentName);
        entity.setTotalCases(totalCases);
        entity.setPassRate(passRate);
        entity.setAvgScore(avgScore);
        entity.setP95DurationMs(p95DurationMs);
        mapper.insert(entity);
        // created_at 由数据库默认值填充，insert 后实体上没有；重新查一次，避免响应里时间为 null
        return find(entity.getBaselineId()).orElseGet(() -> toDomain(entity));
    }

    @Override
    public List<Baseline> findAll() {
        return mapper.selectList(Wrappers.<EvalBaselineEntity>lambdaQuery()
                        .orderByDesc(EvalBaselineEntity::getCreatedAt))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Baseline> find(long baselineId) {
        return Optional.ofNullable(mapper.selectById(baselineId)).map(this::toDomain);
    }

    @Override
    public Optional<Baseline> findByTask(long taskId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<EvalBaselineEntity>lambdaQuery()
                        .eq(EvalBaselineEntity::getTaskId, taskId)))
                .map(this::toDomain);
    }

    private Baseline toDomain(EvalBaselineEntity entity) {
        return new Baseline(entity.getBaselineId(), entity.getName(), entity.getTaskId(),
                entity.getAgentName(), entity.getTotalCases() == null ? 0 : entity.getTotalCases(),
                entity.getPassRate(), entity.getAvgScore(), entity.getP95DurationMs(),
                toInstant(entity.getCreatedAt()));
    }

    private static Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant();
    }
}
