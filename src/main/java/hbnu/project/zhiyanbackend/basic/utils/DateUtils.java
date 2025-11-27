package hbnu.project.zhiyanbackend.basic.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.lang.management.ManagementFactory;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间工具类（基于Java 8+ DateTime API）
 *
 * @author ErgouTree
 * @modify Tsk
 * @rewrit ErgouTree
 */
public class DateUtils {

    // ==================== 日期格式常量 ====================
    public static final String YYYY = "yyyy";
    public static final String YYYY_MM = "yyyy-MM";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    // ==================== 线程安全的DateTimeFormatter ====================
    public static final DateTimeFormatter FORMATTER_YYYY = DateTimeFormatter.ofPattern(YYYY);
    public static final DateTimeFormatter FORMATTER_YYYY_MM = DateTimeFormatter.ofPattern(YYYY_MM);
    public static final DateTimeFormatter FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern(YYYY_MM_DD);
    public static final DateTimeFormatter FORMATTER_YYYYMMDDHHMMSS = DateTimeFormatter.ofPattern(YYYYMMDDHHMMSS);
    public static final DateTimeFormatter FORMATTER_YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern(YYYY_MM_DD_HH_MM_SS);

    // ==================== 当前时间相关方法 ====================

    /**
     * 获取当前Date型日期
     */
    public static Date getNowDate() {
        return Date.from(Instant.now());
    }

    /**
     * 获取当前日期字符串, 默认格式为yyyy-MM-dd
     */
    public static String getDate() {
        return format(LocalDate.now(), FORMATTER_YYYY_MM_DD);
    }

    /**
     * 获取当前时间字符串，格式为yyyy-MM-dd HH:mm:ss
     */
    public static String getTime() {
        return format(LocalDateTime.now(), FORMATTER_YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 获取当前时间字符串，格式为yyyyMMddHHmmss
     */
    public static String dateTimeNow() {
        return format(LocalDateTime.now(), FORMATTER_YYYYMMDDHHMMSS);
    }

    /**
     * 获取当前时间字符串，自定义格式
     */
    public static String dateTimeNow(final String format) {
        return format(LocalDateTime.now(), DateTimeFormatter.ofPattern(format));
    }

    // ==================== 格式化方法 ====================

    /**
     * 格式化LocalDateTime为字符串
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }

    /**
     * 格式化LocalDate为字符串
     */
    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date.format(formatter);
    }

    /**
     * 格式化Date为字符串
     */
    public static String format(Date date, DateTimeFormatter formatter) {
        return format(toLocalDateTime(date), formatter);
    }

    /**
     * 解析字符串为LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateStr, DateTimeFormatter formatter) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDateTime.parse(dateStr, formatter);
    }

    /**
     * 解析字符串为LocalDate
     */
    public static LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr, formatter);
    }

    // ==================== 日期路径相关 ====================

    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static String datePath() {
        return format(LocalDate.now(), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * 日期路径 即年月日 如20180808
     */
    public static String dateTime() {
        return format(LocalDate.now(), DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // ==================== 服务器启动时间 ====================

    /**
     * 获取服务器启动时间
     */
    public static LocalDateTime getServerStartDateTime() {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
    }

    // ==================== 时间差计算 ====================

    /**
     * 计算时间差（优化版本）
     *
     * @param endDateTime   结束时间
     * @param startDateTime 开始时间
     * @return 格式化的时间差字符串
     */
    public static String timeDistance(LocalDateTime endDateTime, LocalDateTime startDateTime) {
        Duration duration = Duration.between(startDateTime, endDateTime);
        return DurationFormatUtils.formatDuration(duration.toMillis(), "d'天'H'小时'm'分钟'", true);
    }

    /**
     * 兼容Date类型的时间差计算
     */
    public static String timeDistance(Date endDate, Date startTime) {
        return timeDistance(toLocalDateTime(endDate), toLocalDateTime(startTime));
    }

    // ==================== 类型转换方法 ====================

    /**
     * LocalDateTime ==> Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDate ==> Date
     */
    public static Date toDate(LocalDate date) {
        return toDate(date.atStartOfDay());
    }

    /**
     * Date ==> LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Date ==> LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        return toLocalDateTime(date).toLocalDate();
    }

    /**
     * Instant ==> LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}