package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID 生成器，容忍时钟回拨。
 *
 * <p>MyBatis-Plus 自带的 {@code DefaultIdentifierGenerator} 在时钟回拨时直接抛
 * {@code Clock moved backwards}，容器里 NTP 校时一次抖动就会让整轮评测任务失败。
 * 这里改用逻辑时钟：{@code timestamp = max(now, lastTimestamp)}，同一毫秒内序号溢出时
 * 逻辑时间 +1，永远单调递增，既不抛异常也不会产生重复 ID。
 *
 * <p>ID 超出 JS 安全整数范围，对外一律以字符串输出，见 {@code IdJson}。
 */
@Component
public class SnowflakeIdGenerator implements IdentifierGenerator {

    /** 起始纪元：2024-01-01T00:00:00Z。 */
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator() {
        this(1L, 1L);
    }

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId < 0 || workerId >= (1L << WORKER_ID_BITS)) {
            throw new IllegalArgumentException("workerId 超出取值范围: " + workerId);
        }
        if (datacenterId < 0 || datacenterId >= (1L << DATACENTER_ID_BITS)) {
            throw new IllegalArgumentException("datacenterId 超出取值范围: " + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    @Override
    public synchronized Number nextId(Object entity) {
        long now = System.currentTimeMillis();
        // 逻辑时钟：时钟回拨时沿用上次时间戳，保证单调递增
        long timestamp = Math.max(now, lastTimestamp);

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 当前毫秒序号用尽，推进逻辑时钟而非等待真实时间
                timestamp = lastTimestamp + 1;
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
}
