package hbnu.project.zhiyanbackend.ocr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.ocr.config.BaiduOCRConfig;
import hbnu.project.zhiyanbackend.ocr.dto.TokenCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 百度OCR服务类
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaiduOcrService {

    private final BaiduOCRConfig baiduOCRConfig;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存Access Token，避免频繁请求
    private static final ConcurrentHashMap<String, TokenCache> TOKEN_CACHE = new ConcurrentHashMap<>();

    /**
     * 识别图片中的文字
     * 通用文字识别-标准版
     *
     * @param file 图片文件
     * @return 识别结果JSON字符串
     */
    public String recognizeText(MultipartFile file) throws IOException {
        // 获取 AccessToken
        String accessToken = getAccessToken();

        // 将图片转换为Base64
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 调用百度OCR API
        return callOcrApi(accessToken, base64Image);
    }


    /**
     * 识别Base64编码的图片
     *
     * @param base64Image Base64编码的图片,可能包含data URI前缀，如data:image/jpg;base64
     * @return 识别结果JSON字符串
     */
    public String recognizeTextFromBase64(String base64Image) throws IOException {
        String accessToken = getAccessToken();
        // 移除data URI前缀（如果存在）
        String cleanBase64 = removeDataUriPrefix(base64Image);
        return callOcrApi(accessToken, cleanBase64);
    }

    /**
     * 移除Base64字符串中的data URI前缀
     * 百度OCR要求base64编码不包含图片头，如data:image/jpg;base64,
     *
     * @param base64Image 可能包含data URI前缀的Base64字符串
     * @return 清理后的Base64字符串
     */
    private String removeDataUriPrefix(String base64Image) {
        if (base64Image == null) {
            return null;
        }
        // 移除data URI前缀（如 data:image/jpg;base64, 或 data:image/png;base64, 等）
        if (base64Image.contains(",")) {
            int commaIndex = base64Image.lastIndexOf(",");
            if (commaIndex >= 0 && commaIndex < base64Image.length() - 1) {
                return base64Image.substring(commaIndex + 1);
            }
        }
        return base64Image;
    }


    /**
     * 获取Access Token
     * Token有效期为30天，需要缓存避免频繁请求
     * https://cloud.baidu.com/doc/OCR/s/Ck3h7y2ia
     * https://cloud.baidu.com/doc/OCR/s/Ck3h7y2ia#%E8%B0%83%E7%94%A8%E6%96%B9%E5%BC%8F%E4%B8%80
     * 调用百度的鉴权接口
     */
    private String getAccessToken() throws IOException {
        String cacheKey = baiduOCRConfig.getApiKey() + "_" + baiduOCRConfig.getSecretKey();
        TokenCache cache = TOKEN_CACHE.get(cacheKey);

        // 检查缓存是否有效
        if (cache != null && !cache.isExpired()) {
            return cache.getAccessToken();
        }

        // 构建请求URL
        String url = String.format("%s?grant_type=client_credentials&client_id=%s&client_secret=%s",
                baiduOCRConfig.getTokenUrl(), baiduOCRConfig.getApiKey(), baiduOCRConfig.getSecretKey());

        // 发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String accessToken = jsonNode.get("access_token").asText();
                long expiresIn = jsonNode.get("expires_in").asLong();

                // 缓存Token（提前2分钟过期）
                TOKEN_CACHE.put(cacheKey, new TokenCache(accessToken, expiresIn - 120));

                log.info("成功获取百度OCR Access Token");
                return accessToken;
            }else {
                throw new IOException("获取Access Token失败: " + response.getBody());
            }
        }catch (ServiceException e){
            log.error("获取Access Token异常", e);
            throw new IOException("获取Access Token异常: " + e.getMessage());
        }
    }


    /**
     * 调用百度OCR API进行文字识别
     * https://cloud.baidu.com/doc/OCR/s/zk3h7xz52
     * 通用文字识别——标准版
     * 
     * 注意：根据百度OCR文档，图片的base64编码不包含图片头（如data:image/jpg;base64,）
     * 图片需要经过base64编码及urlencode后传入
     */
    private String callOcrApi(String accessToken, String base64Image) throws IOException {
        // 确保base64字符串不包含data URI前缀
        String cleanBase64 = removeDataUriPrefix(base64Image);
        
        // 构建请求URL
        String url = baiduOCRConfig.getGeneralBasicUrl() + "?access_token=" + accessToken;

        // 构建请求体，必须 application/x-www-form-urlencoded 格式
        // 根据百度文档，需要对base64字符串进行URL编码
        // 注意：直接构建字符串，避免Spring的MultiValueMap自动编码导致的问题
        String encodedImage = URLEncoder.encode(cleanBase64, StandardCharsets.UTF_8);
        String requestBody = "image=" + encodedImage;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setContentLength(requestBody.length());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try{
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 检查响应中是否包含错误
                String responseBody = response.getBody();
                try {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    if (jsonNode.has("error_code")) {
                        int errorCode = jsonNode.get("error_code").asInt();
                        String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                        log.error("OCR识别失败，错误码: {}, 错误信息: {}", errorCode, errorMsg);
                        throw new IOException("OCR识别失败: " + errorMsg);
                    }
                } catch (Exception e) {
                    // 如果解析失败，可能是响应格式异常，记录日志但继续返回原始响应
                    log.warn("解析OCR响应失败，返回原始响应: {}", responseBody);
                }
                
                log.info("OCR识别成功");
                return responseBody;
            } else {
                throw new IOException("OCR识别失败: " + response.getBody());
            }
        }catch (ServiceException e){
            log.error("OCR识别异常", e);
            throw new IOException("OCR识别异常: " + e.getMessage());
        }
    }


    /**
     * TODO：明天把通用文字识别，高精度版，标准含位置版，高精度含位置版本加上，而且添加对应的传入图片url识别，并且对用户限制使用
     */
}
