package hbnu.project.zhiyanbackend.security.encrypt.annotation;

import hbnu.project.zhiyanbackend.security.encrypt.enumd.AlgorithmType;
import hbnu.project.zhiyanbackend.security.encrypt.enumd.EncodeType;

import java.lang.annotation.*;

/**
 * 请求/响应字段加密注解
 * 标注在 DTO/VO 类的字段上，用于指定该字段在请求/响应时需要加密/解密
 *
 * @author ErgouTree
 * @version 3.0.7
 */
@Documented
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptField {

    /**
     * 加密算法类型
     * 默认使用配置文件中指定的算法
     */
    AlgorithmType algorithm() default AlgorithmType.DEFAULT;

    /**
     * AES/SM4 密钥
     * 如果不指定，则使用配置文件中的默认密钥
     */
    String password() default "";

    /**
     * RSA/SM2 公钥
     * 如果不指定，则使用配置文件中的默认公钥
     */
    String publicKey() default "";

    /**
     * RSA/SM2 私钥
     * 如果不指定，则使用配置文件中的默认私钥
     */
    String privateKey() default "";

    /**
     * 编码方式（BASE64/HEX）
     * 默认使用配置文件中指定的编码方式
     */
    EncodeType encode() default EncodeType.DEFAULT;

    /**
     * 是否在请求时解密
     * 默认 true，表示请求中的该字段需要解密
     */
    boolean decryptRequest() default true;

    /**
     * 是否在响应时加密
     * 默认 true，表示响应中的该字段需要加密
     */
    boolean encryptResponse() default true;
}
