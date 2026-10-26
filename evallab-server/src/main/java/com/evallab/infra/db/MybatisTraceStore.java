package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanEvent;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import com.evallab.core.trace.DuplicateTraceException;
import com.evallab.core.trace.TraceDetail;
import com.evallab.core.trace.TraceSource;
import com.evallab.core.trace.TraceStore;
import com.evallab.infra.db.entity.EvalSpanEntity;
import com.evallab.infra.db.entity.EvalTraceEntity;
import com.evallab.infra.db.mapper.EvalSpanMapper;
import com.evallab.infra.db.mapper.EvalTraceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisTraceStore implements TraceStore {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final TypeReference<List<SpanEvent>> EVENTS_TYPE = new TypeReference<>() {
    };

    private final EvalTraceMapper traceMapper;
    private final EvalSpanMapper spanMapper;
    private final ObjectMapper objectMapper;

    public MybatisTraceStore(EvalTraceMapper traceMapper, EvalSpanMapper spanMapper, ObjectMapper objectMapper) {
        this.traceMapper = traceMapper;
        this.spanMapper = spanMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean existsByTraceId(String traceId) {
        return traceMapper.exists(Wrappers.<EvalTraceEntity>lambdaQuery()
                .eq(EvalTraceEntity::getTraceId, traceId));
    }

    @Override
    @Transactional
    public void save(TraceReport report, TraceSource source) {
        try {
            traceMapper.insert(toEntity(report, source));
        } catch (DuplicateKeyException e) {
            throw new DuplicateTraceException(report.traceId(), e);
        }
        for (SpanDto span : report.spans()) {
            spanMapper.insert(toEntity(report.traceId(), span));
        }
    }

    @Override
    public Optional<TraceDetail> findDetail(String traceId) {
        EvalTraceEntity trace = traceMapper.selectOne(Wrappers.<EvalTraceEntity>lambdaQuery()
                .eq(EvalTraceEntity::getTraceId, traceId));
        if (trace == null) {
            return Optional.empty();
        }
        List<SpanDto> spans = spanMapper.selectList(Wrappers.<EvalSpanEntity>lambdaQuery()
                        .eq(EvalSpanEntity::getTraceId, traceId))
                .stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(SpanDto::startTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return Optional.of(new TraceDetail(
                trace.getTraceId(),
                trace.getAgentName(),
                TraceStatus.valueOf(trace.getStatus()),
                trace.getOutput(),
                trace.getTotalTokens() == null ? 0 : trace.getTotalTokens(),
                trace.getDurationMs() == null ? 0 : trace.getDurationMs(),
                TraceSource.valueOf(trace.getSource()),
                toInstant(trace.getCreatedAt()),
                spans));
    }

    private EvalTraceEntity toEntity(TraceReport report, TraceSource source) {
        EvalTraceEntity entity = new EvalTraceEntity();
        entity.setTraceId(report.traceId());
        entity.setAgentName(report.agentName());
        entity.setStatus(report.status().name());
        entity.setOutput(report.output());
        entity.setTotalTokens(report.totalTokens() == null ? 0 : report.totalTokens());
        entity.setDurationMs(report.durationMs() == null ? 0 : report.durationMs());
        entity.setSource(source.name());
        return entity;
    }

    private EvalSpanEntity toEntity(String traceId, SpanDto span) {
        EvalSpanEntity entity = new EvalSpanEntity();
        entity.setTraceId(traceId);
        entity.setSpanId(span.spanId());
        entity.setParentSpanId(span.parentSpanId());
        entity.setSpanType(span.spanType().name());
        entity.setToolName(span.toolName());
        entity.setInput(span.input());
        entity.setOutput(span.output());
        entity.setError(span.error());
        entity.setTokens(span.tokens() == null ? 0 : span.tokens());
        entity.setDurationMs(span.durationMs() == null ? 0 : span.durationMs());
        entity.setStartTime(toLocal(span.startTime()));
        entity.setEndTime(toLocal(span.endTime()));
        entity.setEvents(writeEvents(span.events()));
        return entity;
    }

    private SpanDto toDto(EvalSpanEntity entity) {
        return new SpanDto(
                entity.getSpanId(),
                entity.getParentSpanId(),
                SpanType.valueOf(entity.getSpanType()),
                entity.getToolName(),
                entity.getInput(),
                entity.getOutput(),
                entity.getError(),
                entity.getTokens(),
                entity.getDurationMs(),
                toInstant(entity.getStartTime()),
                toInstant(entity.getEndTime()),
                readEvents(entity.getEvents()));
    }

    private String writeEvents(List<SpanEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(events);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("events 序列化失败", e);
        }
    }

    private List<SpanEvent> readEvents(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, EVENTS_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("events 反序列化失败", e);
        }
    }

    private static LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
    }

    private static Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant();
    }
}
