package hbnu.project.zhiyanbackend.basic.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花ID工具类（自行实现，脱离Hutool依赖）
 * 生成的ID位数更少，保持API兼容，支持分布式唯一ID生成
 * 完全脱离了Hutool的雪花id算法，完全重写
 *
 * @author ErgouTree
 * @date 2025-09-23
 */
public class SnowflakeIdUtils {

    // ====================== 自定义位段配置（核心：减少位数来缩短ID）======================
    /**
     * 起始时间戳（毫秒）：2025-01-01 00:00:00
     * 用更晚的时间，减少时间戳的绝对值，让ID更短
     */
    private static final long CUSTOM_EPOCH = 1735689600000L;

    /**
     * 工作机器ID的位数（3位，0-7）：原5位，减少2位
     */
    private static final long WORKER_ID_BITS = 3L;

    /**
     * 数据中心ID的位数（3位，0-7）：原5位，减少2位
     */
    private static final long DATACENTER_ID_BITS = 3L;

    /**
     * 序列号的位数（8位，0-255）：原12位，减少4位
     * 注：时间戳+数据中心+工作机器+序列号 总共减少了 2+2+4=8位，再加上起始时间更晚，ID会少4位以上数字
     */
    private static final long SEQUENCE_BITS = 8L;

    // ====================== 位运算最大值计算 ======================
    /**
     * 工作机器ID的最大值：2^3 - 1 = 7
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 数据中心ID的最大值：2^3 - 1 = 7
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号的最大值：2^8 - 1 = 255
     */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // ====================== 位偏移量 ======================
    /**
     * 序列号的位偏移：0位
     */
    private static final long SEQUENCE_SHIFT = 0L;

    /**
     * 工作机器ID的位偏移：序列号位数（8位）
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID的位偏移：工作机器ID位数 + 序列号位数（3+8=11位）
     */
    private static final long DATACENTER_ID_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

    /**
     * 时间戳的位偏移：数据中心ID位数 + 工作机器ID位数 + 序列号位数（3+3+8=14位）
     */
    private static final long TIMESTAMP_SHIFT = DATACENTER_ID_BITS + WORKER_ID_BITS + SEQUENCE_BITS;

    // ====================== 全局变量 ======================
    /**
     * 默认数据中心ID（3位，0-7）
     */
    private static final long DEFAULT_DATACENTER_ID = 1L;

    /**
     * 默认工作机器ID（3位，0-7），通过IP自动获取
     */
    private static final long DEFAULT_WORKER_ID = getWorkerId();

    /**
     * 单例的雪花ID生成器实例
     */
    private static volatile SnowflakeIdGenerator defaultGenerator;

    // 静态初始化默认生成器
    static {
        defaultGenerator = new SnowflakeIdGenerator(DEFAULT_DATACENTER_ID, DEFAULT_WORKER_ID);
    }

    // ====================== 对外API（保持和原代码一致，平滑过渡）======================
    /**
     * 获取默认的雪花ID (Long类型)
     *
     * @return 缩短后的雪花ID
     */
    public static long nextId() {
        return defaultGenerator.nextId();
    }

    /**
     * 获取默认的雪花ID (String类型)
     *
     * @return 雪花ID字符串
     */
    public static String nextIdStr() {
        return String.valueOf(defaultGenerator.nextId());
    }

    /**
     * 创建雪花ID生成器
     *
     * @param datacenterId 数据中心ID (0-7)
     * @param workerId     工作机器ID (0-7)
     * @return 雪花ID生成器实例
     */
    public static SnowflakeIdGenerator createSnowflake(long datacenterId, long workerId) {
        return new SnowflakeIdGenerator(datacenterId, workerId);
    }

    /**
     * 使用指定的数据中心ID和工作机器ID生成雪花ID
     *
     * @param datacenterId 数据中心ID (0-7)
     * @param workerId     工作机器ID (0-7)
     * @return 雪花ID
     */
    public static long nextId(long datacenterId, long workerId) {
        return createSnowflake(datacenterId, workerId).nextId();
    }

    /**
     * 使用指定的数据中心ID和工作机器ID生成雪花ID字符串
     *
     * @param datacenterId 数据中心ID (0-7)
     * @param workerId     工作机器ID (0-7)
     * @return 雪花ID字符串
     */
    public static String nextIdStr(long datacenterId, long workerId) {
        return String.valueOf(nextId(datacenterId, workerId));
    }

    /**
     * 解析雪花ID，获取其组成信息
     *
     * @param snowflakeId 雪花ID
     * @return 雪花ID信息
     */
    public static SnowflakeInfo parseSnowflakeId(long snowflakeId) {
        // 位运算解析各部分
        long timestamp = (snowflakeId >> TIMESTAMP_SHIFT) + CUSTOM_EPOCH;
        long datacenterId = (snowflakeId >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
        long workerId = (snowflakeId >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long sequence = snowflakeId & MAX_SEQUENCE;

        return new SnowflakeInfo(timestamp, datacenterId, workerId, sequence);
    }

    /**
     * 重置默认雪花ID生成器
     *
     * @param datacenterId 数据中心ID
     * @param workerId     工作机器ID
     */
    public static void resetDefaultSnowflake(long datacenterId, long workerId) {
        synchronized (SnowflakeIdUtils.class) {
            defaultGenerator = new SnowflakeIdGenerator(datacenterId, workerId);
        }
    }

    // ====================== 内部工具方法 ======================
    /**
     * 自动获取工作机器ID（0-7）
     * 基于本机IP地址的最后一个字节，取模后限制在0-7范围内
     *
     * @return 工作机器ID (0-7)
     */
    private static long getWorkerId() {
        try {
            // 获取本机IP地址
            InetAddress localHost = InetAddress.getLocalHost();
            byte[] address = localHost.getAddress();
            // 取IP最后一个字节
            int lastByte = address[address.length - 1] & 0xFF;
            // 限制在0-7范围内（3位的最大值）
            return lastByte % (MAX_WORKER_ID + 1);
        } catch (UnknownHostException e) {
            // 如果获取IP失败，使用进程ID取模
            long pid = ProcessHandle.current().pid();
            return pid % (MAX_WORKER_ID + 1);
        }
    }

    // ====================== 内部雪花ID生成器实现类 ======================
    /**
     * 雪花ID生成器核心实现类
     */
    private static class SnowflakeIdGenerator {
        /**
         * 数据中心ID
         */
        private final long datacenterId;

        /**
         * 工作机器ID
         */
        private final long workerId;

        /**
         * 序列号（原子类，保证线程安全）
         */
        private final AtomicLong sequence = new AtomicLong(0L);

        /**
         * 上一次生成ID的时间戳
         */
        private volatile long lastTimestamp = -1L;

        /**
         * 构造函数，校验数据中心ID和工作机器ID的合法性
         *
         * @param datacenterId 数据中心ID (0-7)
         * @param workerId     工作机器ID (0-7)
         */
        public SnowflakeIdGenerator(long datacenterId, long workerId) {
            if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
                throw new IllegalArgumentException(String.format("Datacenter ID must be between 0 and %d", MAX_DATACENTER_ID));
            }
            if (workerId < 0 || workerId > MAX_WORKER_ID) {
                throw new IllegalArgumentException(String.format("Worker ID must be between 0 and %d", MAX_WORKER_ID));
            }
            this.datacenterId = datacenterId;
            this.workerId = workerId;
        }

        /**
         * 生成下一个雪花ID
         *
         * @return 雪花ID
         */
        public synchronized long nextId() {
            long currentTimestamp = System.currentTimeMillis();

            // 校验时间戳：如果当前时间小于上一次生成ID的时间，说明时钟回拨，抛出异常
            if (currentTimestamp < lastTimestamp) {
                throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - currentTimestamp));
            }

            // 如果是同一时间戳，序列号自增
            if (currentTimestamp == lastTimestamp) {
                sequence.set((sequence.get() + 1) & MAX_SEQUENCE);
                // 如果序列号达到最大值，等待下一个时间戳
                if (sequence.get() == 0) {
                    currentTimestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                // 不同时间戳，序列号重置为0
                sequence.set(0L);
            }

            // 更新上一次生成ID的时间戳
            lastTimestamp = currentTimestamp;

            // 计算最终的雪花ID：位运算拼接各部分
            return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT) // 时间戳部分
                    | (datacenterId << DATACENTER_ID_SHIFT) // 数据中心ID部分
                    | (workerId << WORKER_ID_SHIFT) // 工作机器ID部分
                    | sequence.get(); // 序列号部分
        }

        /**
         * 等待下一个毫秒，直到获取到新的时间戳
         *
         * @param lastTimestamp 上一次生成ID的时间戳
         * @return 新的时间戳
         */
        private long waitNextMillis(long lastTimestamp) {
            long currentTimestamp = System.currentTimeMillis();
            while (currentTimestamp <= lastTimestamp) {
                currentTimestamp = System.currentTimeMillis();
            }
            return currentTimestamp;
        }
    }

    // ====================== 雪花ID信息类 ======================
    /**
     * 雪花ID信息类，用于解析ID后的返回结果
     */
    public static class SnowflakeInfo {
        private final long timestamp;
        private final long datacenterId;
        private final long workerId;
        private final long sequence;

        public SnowflakeInfo(long timestamp, long datacenterId, long workerId, long sequence) {
            this.timestamp = timestamp;
            this.datacenterId = datacenterId;
            this.workerId = workerId;
            this.sequence = sequence;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getDatacenterId() {
            return datacenterId;
        }

        public long getWorkerId() {
            return workerId;
        }

        public long getSequence() {
            return sequence;
        }

        @Override
        public String toString() {
            return "SnowflakeInfo{" +
                    "timestamp=" + timestamp +
                    ", datacenterId=" + datacenterId +
                    ", workerId=" + workerId +
                    ", sequence=" + sequence +
                    '}';
        }
    }
}