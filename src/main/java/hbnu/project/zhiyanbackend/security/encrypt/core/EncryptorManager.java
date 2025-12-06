package hbnu.project.zhiyanbackend.security.encrypt.core;

import cn.hutool.core.util.ReflectUtil;
import hbnu.project.zhiyanbackend.security.encrypt.annotation.EncryptField;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 加密器管理器
 * 负责管理和缓存所有的加密器实例，以及缓存需要加密的字段信息
 *
 * @author ErgouTree
 * @version 3.0.0
 */
@Slf4j
@NoArgsConstructor
public class EncryptorManager {

    /**
     * 加密器实例缓存
     * Key: 加密上下文的 hashCode
     * Value: 加密器实例
     */
    private final Map<Integer, IEncryptor> encryptorMap = new ConcurrentHashMap<>();

    /**
     * 类加密字段缓存
     * Key: DTO/VO 类 Class
     * Value: 该类中标注了 @EncryptField 注解的字段集合
     */
    private final Map<Class<?>, Set<Field>> fieldCache = new ConcurrentHashMap<>();

    /**
     * 获取类的加密字段缓存（懒加载）
     * 如果缓存中没有，则扫描并缓存
     *
     * @param sourceClazz DTO/VO 类
     * @return 加密字段集合
     */
    public Set<Field> getFieldCache(Class<?> sourceClazz) {
        if (sourceClazz == null) {
            return new HashSet<>();
        }
        
        // 如果缓存中没有，则扫描并缓存
        if (!fieldCache.containsKey(sourceClazz)) {
            Set<Field> fields = getEncryptFieldSetFromClazz(sourceClazz);
            if (!fields.isEmpty()) {
                fieldCache.put(sourceClazz, fields);
                log.debug("缓存加密字段: {} - {} 个字段", sourceClazz.getSimpleName(), fields.size());
            } else {
                // 即使没有字段也缓存，避免重复扫描
                fieldCache.put(sourceClazz, new HashSet<>());
            }
        }
        
        return fieldCache.get(sourceClazz);
    }

    /**
     * 注册并获取加密器
     * 如果缓存中已存在，则直接返回；否则创建新实例并缓存
     *
     * @param encryptContext 加密上下文
     * @return 加密器实例
     */
    public IEncryptor registAndGetEncryptor(EncryptContext encryptContext) {
        int key = encryptContext.hashCode();
        if (encryptorMap.containsKey(key)) {
            return encryptorMap.get(key);
        }

        // 使用反射创建加密器实例
        IEncryptor encryptor = ReflectUtil.newInstance(
            encryptContext.getAlgorithm().getClazz(),
            encryptContext
        );

        encryptorMap.put(key, encryptor);
        log.debug("注册加密器: {}", encryptContext.getAlgorithm());
        return encryptor;
    }

    /**
     * 移除缓存中的加密器
     *
     * @param encryptContext 加密上下文
     */
    public void removeEncryptor(EncryptContext encryptContext) {
        int key = encryptContext.hashCode();
        encryptorMap.remove(key);
        log.debug("移除加密器: {}", encryptContext.getAlgorithm());
    }

    /**
     * 加密字符串
     *
     * @param value          待加密的值
     * @param encryptContext 加密上下文
     * @return 加密后的字符串
     */
    public String encrypt(String value, EncryptContext encryptContext) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        try {
            IEncryptor encryptor = registAndGetEncryptor(encryptContext);
            return encryptor.encrypt(value, encryptContext.getEncode());
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密字符串
     *
     * @param value          待解密的值
     * @param encryptContext 加密上下文
     * @return 解密后的字符串
     */
    public String decrypt(String value, EncryptContext encryptContext) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        try {
            IEncryptor encryptor = registAndGetEncryptor(encryptContext);
            return encryptor.decrypt(value);
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取一个类中所有标注了 @EncryptField 注解的字段
     *
     * @param clazz DTO/VO 类
     * @return 加密字段集合
     */
    private Set<Field> getEncryptFieldSetFromClazz(Class<?> clazz) {
        Set<Field> fieldSet = new HashSet<>();

        // 过滤接口、内部类、匿名类
        if (clazz.isInterface() || clazz.isMemberClass() || clazz.isAnonymousClass()) {
            return fieldSet;
        }

        // 遍历类及其父类，获取所有字段
        Class<?> currentClazz = clazz;
        while (currentClazz != null) {
            Field[] fields = currentClazz.getDeclaredFields();
            fieldSet.addAll(Arrays.asList(fields));
            currentClazz = currentClazz.getSuperclass();
        }

        // 过滤出标注了 @EncryptField 注解且类型为 String 的字段
        fieldSet = fieldSet.stream()
            .filter(field -> field.isAnnotationPresent(EncryptField.class))
            .filter(field -> field.getType() == String.class)
            .collect(Collectors.toSet());

        // 设置字段可访问
        fieldSet.forEach(field -> field.setAccessible(true));

        return fieldSet;
    }

    /**
     * 检查类是否有加密字段
     *
     * @param clazz DTO/VO 类
     * @return 是否有加密字段
     */
    public boolean hasEncryptFields(Class<?> clazz) {
        Set<Field> fields = getFieldCache(clazz);
        return fields != null && !fields.isEmpty();
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        encryptorMap.clear();
        fieldCache.clear();
        log.info("清空加密器缓存");
    }

    /**
     * 获取缓存的加密器数量
     *
     * @return 加密器数量
     */
    public int getEncryptorCount() {
        return encryptorMap.size();
    }

    /**
     * 获取缓存的类数量
     *
     * @return 类数量
     */
    public int getFieldCacheCount() {
        return fieldCache.size();
    }
}
