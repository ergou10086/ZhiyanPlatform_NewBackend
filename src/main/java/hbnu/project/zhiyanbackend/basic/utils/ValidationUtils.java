package hbnu.project.zhiyanbackend.basic.utils;

import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * 成果部分用的校验工具
 * 尝试性使用
 *
 * @author ErgouTree
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * 校验ID不能为空
     *
     * @param id ID值
     * @param fieldName 字段名称（用于异常提示）
     * @return 校验通过的ID
     * @throws ServiceException ID为空时抛出
     */
    public static Long requireId(Long id, String fieldName) {
        if (id == null) {
            throw new ServiceException(fieldName + "不能为空");
        }
        return id;
    }

    /**
     * 校验集合不能为空
     *
     * @param collection 集合
     * @param fieldName 字段名称（用于异常提示）
     * @param <T> 集合元素类型
     * @return 校验通过的集合
     * @throws ServiceException 集合为空时抛出
     */
    public static <T> Collection<T> requireNonEmpty(Collection<T> collection, String fieldName) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        return collection;
    }

    /**
     * 断言表达式为真
     *
     * @param expression 布尔表达式
     * @param message 异常消息
     * @throws ServiceException 表达式为假时抛出
     */
    public static void assertTrue(boolean expression, String message) {
        if (!expression) {
            throw new ServiceException(message);
        }
    }

    /**
     * 校验对象不能为空
     *
     * @param target 目标对象
     * @param message 异常消息
     * @param <T> 对象类型
     * @return 校验通过的对象
     * @throws ServiceException 对象为空时抛出
     */
    public static <T> T requireNonNull(T target, String message) {
        if (Objects.isNull(target)) {
            throw new ServiceException(message);
        }
        return target;
    }

    /**
     * 校验字符串不能为空
     *
     * @param str 字符串
     * @param fieldName 字段名称（用于异常提示）
     * @return 校验通过的字符串
     * @throws ServiceException 字符串为空时抛出
     */
    public static String requireNonBlank(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new ServiceException(fieldName + "不能为空");
        }
        return str;
    }

    /**
     * 校验数值必须大于0
     *
     * @param value 数值
     * @param fieldName 字段名称（用于异常提示）
     * @return 校验通过的数值
     * @throws ServiceException 数值小于等于0时抛出
     */
    public static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new ServiceException(fieldName + "必须大于0");
        }
        return value;
    }

    /**
     * 校验数值必须大于等于0
     *
     * @param value 数值
     * @param fieldName 字段名称（用于异常提示）
     * @return 校验通过的数值
     * @throws ServiceException 数值小于0时抛出
     */
    public static Long requireNonNegative(Long value, String fieldName) {
        if (value == null || value < 0) {
            throw new ServiceException(fieldName + "不能为负数");
        }
        return value;
    }

    /**
     * 校验集合大小是否在指定范围内
     *
     * @param collection 集合
     * @param min 最小大小
     * @param max 最大大小
     * @param fieldName 字段名称（用于异常提示）
     * @param <T> 集合元素类型
     * @return 校验通过的集合
     * @throws ServiceException 集合大小不在范围内时抛出
     */
    public static <T> Collection<T> requireSizeInRange(Collection<T> collection, int min, int max, String fieldName) {
        if (collection == null) {
            throw new ServiceException(fieldName + "不能为空");
        }
        int size = collection.size();
        if (size < min || size > max) {
            throw new ServiceException(fieldName + "的数量必须在" + min + "到" + max + "之间");
        }
        return collection;
    }
}