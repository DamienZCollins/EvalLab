package com.evallab.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliArgsTest {

    @Test
    void parsesRequiredAndOptionalArgs() {
        CliArgs args = CliArgs.parse(new String[]{
                "--task", "1", "--baseline", "2", "--threshold-pass-rate", "-0.10",
                "--threshold-p95-duration", "1.5", "--server", "http://host:9000/"});
        assertEquals(1L, args.taskId());
        assertEquals(2L, args.baselineId());
        assertEquals("-0.10", args.passRate());
        assertEquals("1.5", args.p95Duration());
        assertNull(args.avgScore());
        // 末尾斜杠去掉，避免拼出 //api/eval/gate
        assertEquals("http://host:9000", args.server());
    }

    @Test
    void acceptsInlineEqualsForm() {
        CliArgs args = CliArgs.parse(new String[]{"--task=11", "--baseline=22"});
        assertEquals(11L, args.taskId());
        assertEquals(22L, args.baselineId());
        assertEquals("http://localhost:8080", args.server());
    }

    @Test
    void rejectsMissingRequired() {
        assertThrows(CliArgs.InvalidArgumentException.class,
                () -> CliArgs.parse(new String[]{"--task", "1"}));
    }

    @Test
    void rejectsUnknownFlag() {
        assertThrows(CliArgs.InvalidArgumentException.class,
                () -> CliArgs.parse(new String[]{"--task", "1", "--baseline", "2", "--nope"}));
    }

    @Test
    void rejectsNonNumericId() {
        assertThrows(CliArgs.InvalidArgumentException.class,
                () -> CliArgs.parse(new String[]{"--task", "abc", "--baseline", "2"}));
    }

    @Test
    void rejectsFlagWithoutValue() {
        assertThrows(CliArgs.InvalidArgumentException.class,
                () -> CliArgs.parse(new String[]{"--task", "1", "--baseline"}));
    }
}
