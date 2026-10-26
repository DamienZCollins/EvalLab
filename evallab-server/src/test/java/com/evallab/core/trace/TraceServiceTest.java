package com.evallab.core.trace;

import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceServiceTest {

    private final InMemoryStore store = new InMemoryStore();
    private final TraceService service = new TraceService(store);

    @Test
    void savesNewTrace() {
        TraceIngestResult result = service.report(validReport());

        assertThat(result.duplicated()).isFalse();
        assertThat(store.saved).isEqualTo(1);
    }

    @Test
    void duplicateTraceIdIsIgnoredNotOverwritten() {
        service.report(validReport());
        TraceIngestResult second = service.report(validReport());

        assertThat(second.duplicated()).isTrue();
        assertThat(store.saved).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateIsTreatedAsDuplicated() {
        store.throwDuplicate = true;

        assertThat(service.report(validReport()).duplicated()).isTrue();
    }

    @Test
    void invalidTraceIsRejected() {
        TraceReport invalid = new TraceReport("", "agent", TraceStatus.SUCCESS, "o", 0, 0L, List.of());

        assertThatThrownBy(() -> service.report(invalid))
                .isInstanceOf(InvalidTraceException.class);
        assertThat(store.saved).isZero();
    }

    @Test
    void missingTraceThrowsNotFound() {
        assertThatThrownBy(() -> service.get("ghost"))
                .isInstanceOf(TraceNotFoundException.class);
    }

    private static TraceReport validReport() {
        Instant start = Instant.parse("2026-10-26T10:00:00Z");
        SpanDto span = new SpanDto("a", null, SpanType.LLM_CALL, null, "in", "out", null, 1, 10L,
                start, start.plusMillis(10), List.of());
        return new TraceReport("t1", "agent", TraceStatus.SUCCESS, "out", 10, 100L, List.of(span));
    }

    private static final class InMemoryStore implements TraceStore {

        private final java.util.Set<String> ids = new java.util.HashSet<>();
        private int saved;
        private boolean throwDuplicate;

        @Override
        public boolean existsByTraceId(String traceId) {
            return ids.contains(traceId);
        }

        @Override
        public void save(TraceReport report, TraceSource source) {
            if (throwDuplicate) {
                throw new DuplicateTraceException(report.traceId(), null);
            }
            ids.add(report.traceId());
            saved++;
        }

        @Override
        public Optional<TraceDetail> findDetail(String traceId) {
            return Optional.empty();
        }
    }
}
