package com.evallab.infra.db;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    @Test
    void 同一毫秒内连续生成不重复且单调递增() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        long previous = -1;
        for (int i = 0; i < 10_000; i++) {
            long id = generator.nextId(null).longValue();
            assertTrue(ids.add(id), "重复 ID: " + id);
            assertTrue(id > previous, "非单调递增: " + previous + " -> " + id);
            previous = id;
        }
        assertEquals(10_000, ids.size());
    }

    @Test
    void 序号溢出时推进逻辑时钟而不是抛异常() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        // 忙循环撞满同一毫秒的 4096 个序号，继续生成不得抛异常
        long previous = -1;
        for (int i = 0; i < 20_000; i++) {
            long id = generator.nextId(null).longValue();
            assertTrue(id > previous);
            previous = id;
        }
    }

    @Test
    void 参数越界时启动即失败() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(32, 1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(1, 32));
    }
}
