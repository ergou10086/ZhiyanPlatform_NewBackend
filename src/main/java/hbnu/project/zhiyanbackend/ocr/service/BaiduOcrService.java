package hbnu.project.zhiyanbackend.ocr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.ocr.config.BaiduOCRConfig;
import hbnu.project.zhiyanbackend.ocr.model.dto.TokenCache;
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
 * 四个OCR接口，每个接口对应两种调用方式
 * 带限额
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
     * 上传文件
     * 通用文字识别-标准版
     *
     * @param file 图片文件
     * @return 识别结果JSON字符串
     */
    public String recognizeTextFileStandard(MultipartFile file) throws IOException {
        String accessToken = getAccessToken();
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return callOcrApi(accessToken, base64Image, null, "general_basic");
    }

    /**
     * 识别图片中的文字
     * 上传文件
     * 通用文字识别-标准版含位置
     *
     * @param file 图片文件
     * @return 识别结果JSON字符串
     */
    public String recognizeTextFileStandardWithPos(MultipartFile file) throws IOException {
        String accessToken = getAccessToken();
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return callOcrApi(accessToken, base64Image, null, "general");
    }

    /**
     * 识别图片中的文字
     * 上传文件
     * 通用文字识别-高精度版
     *
     * @param file 图片文件
     * @return 识别结果JSON字符串
     */
    public String recognizeTextFileHighAccuracy(MultipartFile file) throws IOException {
        String accessToken = getAccessToken();
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return callOcrApi(accessToken, base64Image, null, "accurate_basic");
    }

    /**
     * 识别图片中的文字
     * 上传文件
     * 通用文字识别-高精度含位置版
     *
     * @param file 图片文件
     * @return 识别结果JSON字符串
     */
    public String recognizeTextFileHighAccuracyWithPos(MultipartFile file) throws IOException {
        String accessToken = getAccessToken();
        byte[] imageBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        return callOcrApi(accessToken, base64Image, null, "accurate");
    }

    /**
     * 识别图片中的文字
     * 使用url，用于在知识库等已经上传了图片的地方提供ocr识别
     * 通用文字识别——标准版
     *
     * @param url 图片文件url，对于本项目就是COS图片访问的url
     * @return 识别结果JSON字符串
     */
    public String recognizeTextUrlStandard(String url) throws IOException {
        String accessToken = getAccessToken();
        return callOcrApi(accessToken, null, url, "general_basic");
    }

    /**
     * 识别图片中的文字
     * 使用url，用于在知识库等已经上传了图片的地方提供ocr识别
     * 通用文字识别——标准版含位置
     *
     * @param url 图片文件url，对于本项目就是COS图片访问的url
     * @return 识别结果JSON字符串
     */
    public String recognizeTextUrlStandardWithPos(String url) throws IOException {
        String accessToken = getAccessToken();
        return callOcrApi(accessToken, null, url, "general");
    }

    /**
     * 识别图片中的文字
     * 使用url，用于在知识库等已经上传了图片的地方提供ocr识别
     * 通用文字识别——高精度版
     *
     * @param url 图片文件url，对于本项目就是COS图片访问的url
     * @return 识别结果JSON字符串
     */
    public String recognizeTextUrlHighAccuracy(String url) throws IOException {
        String accessToken = getAccessToken();
        return callOcrApi(accessToken, null, url, "accurate_basic");
    }

    /**
     * 识别图片中的文字
     * 使用url，用于在知识库等已经上传了图片的地方提供ocr识别
     * 通用文字识别——高精度版含位置
     *
     * @param url 图片文件url，对于本项目就是COS图片访问的url
     * @return 识别结果JSON字符串
     */
    public String recognizeTextUrlHighAccuracyWithPos(String url) throws IOException {
        String accessToken = getAccessToken();
        return callOcrApi(accessToken, null, url, "accurate");
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

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String accessToken = jsonNode.get("access_token").asText();
                long expiresIn = jsonNode.get("expires_in").asLong();

                // 缓存Token（提前2分钟过期）
                TOKEN_CACHE.put(cacheKey, new TokenCache(accessToken, System.currentTimeMillis() + (expiresIn - 120) * 1000));

                log.info("成功获取百度OCR Access Token");
                return accessToken;
            } else {
                throw new IOException("获取Access Token失败: " + response.getBody());
            }
        } catch (ServiceException e) {
            log.error("获取Access Token异常", e);
            throw new IOException("获取Access Token异常: " + e.getMessage());
        }
    }

    /**
     * 调用百度OCR API进行文字识别
     * 统一的调用方法，支持四种识别类型
     *
     * @param accessToken 访问令牌
     * @param base64Image base64编码的图片（与url二选一）
     * @param imageUrl    图片URL（与base64Image二选一）
     * @param apiType     API类型: general_basic, general, accurate_basic, accurate
     * @return 识别结果JSON字符串
     */
    private String callOcrApi(String accessToken, String base64Image, String imageUrl, String apiType) throws IOException {
        // 根据apiType构建URL
        String apiUrl = getApiUrl(apiType);
        String url = apiUrl + "?access_token=" + accessToken;

        // 构建请求体
        String requestBody;
        if (base64Image != null) {
            // 使用base64图片
            String cleanBase64 = removeDataUriPrefix(base64Image);
            String encodedImage = URLEncoder.encode(cleanBase64, StandardCharsets.UTF_8);
            requestBody = "image=" + encodedImage;
        } else if (imageUrl != null) {
            // 使用图片URL
            String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
            requestBody = "url=" + encodedUrl;
        } else {
            throw new IOException("必须提供base64Image或imageUrl其中之一");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setContentLength(requestBody.length());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody();

                // 检查响应中是否包含错误
                try {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    if (jsonNode.has("error_code")) {
                        int errorCode = jsonNode.get("error_code").asInt();
                        String errorMsg = jsonNode.has("error_msg") ? jsonNode.get("error_msg").asText() : "未知错误";
                        log.error("OCR识别失败，错误码: {}, 错误信息: {}", errorCode, errorMsg);
                        throw new IOException("OCR识别失败: " + errorMsg);
                    }
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("解析OCR响应失败，返回原始响应: {}", responseBody);
                }

                log.info("OCR识别成功，使用API: {}", apiType);
                return responseBody;
            } else {
                throw new IOException("OCR识别失败: " + response.getBody());
            }
        } catch (ServiceException e) {
            log.error("OCR识别异常", e);
            throw new IOException("OCR识别异常: " + e.getMessage());
        }
    }

    /**
     * 根据API类型获取对应的URL
     *
     * @param apiType API类型
     * @return API URL
     */
    private String getApiUrl(String apiType) {
        String baseUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/";
        switch (apiType) {
            case "general_basic":
                return baseUrl + "general_basic";
            case "general":
                return baseUrl + "general";
            case "accurate_basic":
                return baseUrl + "accurate_basic";
            case "accurate":
                return baseUrl + "accurate";
            default:
                return baseUrl + "general_basic";
        }
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
            int commaIndex = base64Image.indexOf(",");
            if (commaIndex >= 0 && commaIndex < base64Image.length() - 1) {
                return base64Image.substring(commaIndex + 1);
            }
        }
        return base64Image;
    }
}