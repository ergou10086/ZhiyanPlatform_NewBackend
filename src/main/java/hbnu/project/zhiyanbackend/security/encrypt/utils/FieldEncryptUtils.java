package hbnu.project.zhiyanbackend.security.encrypt.utils;

import hbnu.project.zhiyanbackend.security.encrypt.annotation.EncryptField;
import hbnu.project.zhiyanbackend.security.encrypt.config.properties.EncryptorProperties;
import hbnu.project.zhiyanbackend.security.encrypt.core.EncryptContext;
import hbnu.project.zhiyanbackend.security.encrypt.core.EncryptorManager;
import hbnu.project.zhiyanbackend.security.encrypt.enumd.AlgorithmType;
import hbnu.project.zhiyanbackend.security.encrypt.enumd.EncodeType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * 字段加密工具类
 * 用于处理请求/响应中指定字段的加密和解密
 *
 * @author ErgouTree
 * @version 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldEncryptUtils {

    private final EncryptorProperties encryptorProperties;
    private final EncryptorManager encryptorManager;
    private static final ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    /**
     * 解密请求体中的字段
     *
     * @param jsonBody JSON字符串
     * @param clazz    目标类型
     * @return 解密后的JSON字符串
     */
    public String decryptRequestFields(String jsonBody, Class<?> clazz) {
        if (StringUtils.isBlank(jsonBody) || clazz == null) {
            return jsonBody;
        }

        try{
            // 检查是否有加密字段
            if(!encryptorManager.hasEncryptFields(clazz)){
                return jsonBody;
            }

            JsonNode rootNode = objectMapper.readTree(jsonBody);
            Set<Field> encryptFields = encryptorManager.getFieldCache(clazz);

            // 处理对象
            if (rootNode.isObject()) {
                decryptObjectNode((ObjectNode) rootNode, encryptFields);
            }
            // 处理数组
            else if (rootNode.isArray()) {
                decryptArrayNode((ArrayNode) rootNode, encryptFields);
            }

            return objectMapper.writeValueAsString(rootNode);
        }catch (Exception e){
            log.error("解密请求字段失败", e);
            // 解密失败返回原内容
            return jsonBody;
        }
    }

    /**
     * 加密响应体中的字段
     *
     * @param jsonBody JSON字符串
     * @param clazz    目标类型
     * @return 加密后的JSON字符串
     */
    public String encryptResponseFields(String jsonBody, Class<?> clazz) {
        if (StringUtils.isBlank(jsonBody) || clazz == null) {
            return jsonBody;
        }

        try {
            // 检查是否有加密字段
            if (!encryptorManager.hasEncryptFields(clazz)) {
                return jsonBody;
            }

            JsonNode rootNode = objectMapper.readTree(jsonBody);
            Set<Field> encryptFields = encryptorManager.getFieldCache(clazz);

            // 处理对象
            if (rootNode.isObject()) {
                encryptObjectNode((ObjectNode) rootNode, encryptFields);
            }
            // 处理数组
            else if (rootNode.isArray()) {
                encryptArrayNode((ArrayNode) rootNode, encryptFields);
            }

            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            log.error("加密响应字段失败", e);
            // 加密失败返回原内容
            return jsonBody;
        }
    }

    /**
     * 解密对象节点
     */
    private void decryptObjectNode(ObjectNode objectNode, Set<Field> encryptFields) {
        for (Field field : encryptFields) {
            EncryptField encryptField = field.getAnnotation(EncryptField.class);
            if (encryptField == null || !encryptField.decryptRequest()) {
                continue;
            }

            String fieldName = field.getName();
            JsonNode fieldNode = objectNode.get(fieldName);

            if (fieldNode != null && fieldNode.isTextual()) {
                String encryptedValue = fieldNode.asText();
                if (StringUtils.isNotBlank(encryptedValue)) {
                    String decryptedValue = decryptField(encryptedValue, field, encryptField);
                    objectNode.put(fieldName, decryptedValue);
                }
            }
        }
    }

    /**
     * 加密对象节点
     */
    private void encryptObjectNode(ObjectNode objectNode, Set<Field> encryptFields) {
        for (Field field : encryptFields) {
            EncryptField encryptField = field.getAnnotation(EncryptField.class);
            if (encryptField == null || !encryptField.encryptResponse()) {
                continue;
            }

            String fieldName = field.getName();
            JsonNode fieldNode = objectNode.get(fieldName);

            if (fieldNode != null && fieldNode.isTextual()) {
                String plainValue = fieldNode.asText();
                if (StringUtils.isNotBlank(plainValue)) {
                    String encryptedValue = encryptField(plainValue, field, encryptField);
                    objectNode.put(fieldName, encryptedValue);
                }
            }
        }
    }

    /**
     * 解密数组节点
     */
    private void decryptArrayNode(ArrayNode arrayNode, Set<Field> encryptFields) {
        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                decryptObjectNode((ObjectNode) item, encryptFields);
            }
        }
    }

    /**
     * 加密数组节点
     */
    private void encryptArrayNode(ArrayNode arrayNode, Set<Field> encryptFields) {
        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                encryptObjectNode((ObjectNode) item, encryptFields);
            }
        }
    }

    /**
     * 解密字段值
     */
    private String decryptField(String value, Field field, EncryptField encryptField) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        EncryptContext encryptContext = buildEncryptContext(encryptField);
        return encryptorManager.decrypt(value, encryptContext);
    }

    /**
     * 加密字段值
     */
    private String encryptField(String value, Field field, EncryptField encryptField) {
        if (StringUtils.isBlank(value)) {
            return value;
        }

        EncryptContext encryptContext = buildEncryptContext(encryptField);
        return encryptorManager.encrypt(value, encryptContext);
    }

    /**
     * 构建加密上下文
     */
    private EncryptContext buildEncryptContext(EncryptField encryptField) {
        EncryptContext context = new EncryptContext();

        // 算法
        AlgorithmType algorithm = encryptField.algorithm() == AlgorithmType.DEFAULT
                ? encryptorProperties.getAlgorithm() : encryptField.algorithm();
        context.setAlgorithm(algorithm);

        // 编码方式
        EncodeType encode = encryptField.encode() == EncodeType.DEFAULT
                ? encryptorProperties.getEncode() : encryptField.encode();
        context.setEncode(encode);

        // 密钥
        String password = StringUtils.isBlank(encryptField.password())
                ? encryptorProperties.getPassword() : encryptField.password();
        context.setPassword(password);

        // 公钥
        String publicKey = StringUtils.isBlank(encryptField.publicKey())
                ? encryptorProperties.getPublicKey() : encryptField.publicKey();
        context.setPublicKey(publicKey);

        // 私钥
        String privateKey = StringUtils.isBlank(encryptField.privateKey())
                ? encryptorProperties.getPrivateKey() : encryptField.privateKey();
        context.setPrivateKey(privateKey);

        return context;
    }
}
