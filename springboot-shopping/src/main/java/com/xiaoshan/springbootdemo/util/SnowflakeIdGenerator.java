package com.xiaoshan.springbootdemo.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 */
@Component
public class SnowflakeIdGenerator {

    // 起始时间戳（2024-01-01 00:00:00）
    private final long START_TIMESTAMP = 1704067200000L;

    // 机器ID所占位数
    private final long WORKER_ID_BITS = 5L;
    // 数据中心ID所占位数
    private final long DATA_CENTER_ID_BITS = 5L;
    // 序列号所占位数
    private final long SEQUENCE_BITS = 12L;

    // 机器ID最大值（31）
    private final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 数据中心ID最大值（31）
    private final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    // 序列号最大值（4095）
    private final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // 移位偏移量
    private final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    // 机器ID（0-31）
    private long workerId;
    // 数据中心ID（0-31）
    private long dataCenterId;
    // 序列号
    private long sequence = 0L;
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        this.workerId = 1L;
        this.dataCenterId = 1L;
    }

    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId不能大于31或小于0");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("dataCenterId不能大于31或小于0");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    /**
     * 生成雪花ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨处理
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 小于5毫秒，等待时间追上
                try {
                    wait(offset << 1);
                    timestamp = System.currentTimeMillis();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException("时钟回拨超过限制");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("时钟回拨超过限制");
            }
        }

        // 同一毫秒内，序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号用完，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组合ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 生成订单号（带前缀）
     */
    public String generateOrderNumber() {
        return "ORD" + nextId();
    }

    /**
     * 从雪花ID中解析时间戳
     */
    public long parseTimestamp(long snowflakeId) {
        return (snowflakeId >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
    }
}