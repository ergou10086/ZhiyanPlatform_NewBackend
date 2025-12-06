package hbnu.project.zhiyanbackend.security.encrypt.annotation;

import java.lang.annotation.*;

/**
 * API 接口加密注解
 * 标注在 Controller 方法上，自动对请求和响应进行加解密
 *
 * @author ErgouTree
 * @version 3.0.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEncrypt {

    /**
     * 加密模式
     * FULL: 全量加密（对整个请求/响应体加密）
     * FIELD: 字段加密（只对标注了@EncryptField的字段加密）
     */
    EncryptMode mode() default EncryptMode.FIELD;

    /**
     * 是否加密请求数据
     * 默认不加密请求
     */
    boolean request() default false;

    /**
     * 是否加密响应数据
     * 默认加密响应
     */
    boolean response() default true;

    /**
     * 是否加密请求数据（别名，兼容旧版本）
     */
    boolean requestEncrypt() default false;

    /**
     * 是否加密响应数据（别名，兼容旧版本）
     */
    boolean responseEncrypt() default true;

    /**
     * 加密模式枚举
     */
    enum EncryptMode {
        /**
         * 全量加密：对整个请求/响应体进行加密
         */
        FULL,
        /**
         * 字段加密：只对标注了@EncryptField的字段进行加密
         */
        FIELD
    }
}