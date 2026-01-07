package hbnu.project.zhiyanbackend.ocr.controller;

import hbnu.project.zhiyanbackend.ocr.service.BaiduOcrService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百度OCR控制器
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/ocr")
@CrossOrigin(origins = {
        "http://zyplatform.xyz",
        "https://zyplatform.xyz",
        "http://api.zyplatform.xyz",
        "https://api.zyplatform.xyz"
})
@RequiredArgsConstructor
public class BaiduOcrController {

    private final BaiduOcrService baiduOcrService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传图片进行文字识别 - 标准版
     *
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/img-recognize/file/standard")
    public ResponseEntity<Map<String, Object>> recognizeFileStandard(@RequestParam("file") MultipartFile file) {
        return processFileRecognition(file, "standard");
    }

    /**
     * 上传图片进行文字识别 - 标准版含位置
     *
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/img-recognize/file/standard-pos")
    public ResponseEntity<Map<String, Object>> recognizeFileStandardWithPos(@RequestParam("file") MultipartFile file) {
        return processFileRecognition(file, "standard-pos");
    }

    /**
     * 上传图片进行文字识别 - 高精度版
     *
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/img-recognize/file/high-accuracy")
    public ResponseEntity<Map<String, Object>> recognizeFileHighAccuracy(@RequestParam("file") MultipartFile file) {
        return processFileRecognition(file, "high-accuracy");
    }

    /**
     * 上传图片进行文字识别 - 高精度含位置版
     *
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/img-recognize/file/high-accuracy-pos")
    public ResponseEntity<Map<String, Object>> recognizeFileHighAccuracyWithPos(@RequestParam("file") MultipartFile file) {
        return processFileRecognition(file, "high-accuracy-pos");
    }

    /**
     * 通过URL进行文字识别 - 标准版
     *
     * @param request 包含url的请求体
     * @return 识别结果
     */
    @PostMapping("/img-recognize/url/standard")
    public ResponseEntity<Map<String, Object>> recognizeUrlStandard(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        return processUrlRecognition(url, "standard");
    }

    /**
     * 通过URL进行文字识别 - 标准版含位置
     *
     * @param request 包含url的请求体
     * @return 识别结果
     */
    @PostMapping("/img-recognize/url/standard-pos")
    public ResponseEntity<Map<String, Object>> recognizeUrlStandardWithPos(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        return processUrlRecognition(url, "standard-pos");
    }

    /**
     * 通过URL进行文字识别 - 高精度版
     *
     * @param request 包含url的请求体
     * @return 识别结果
     */
    @PostMapping("/img-recognize/url/high-accuracy")
    public ResponseEntity<Map<String, Object>> recognizeUrlHighAccuracy(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        return processUrlRecognition(url, "high-accuracy");
    }

    /**
     * 通过URL进行文字识别 - 高精度含位置版
     *
     * @param request 包含url的请求体
     * @return 识别结果
     */
    @PostMapping("/img-recognize/url/high-accuracy-pos")
    public ResponseEntity<Map<String, Object>> recognizeUrlHighAccuracyWithPos(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        return processUrlRecognition(url, "high-accuracy-pos");
    }

    /**
     * 处理文件识别请求
     *
     * @param file 图片文件
     * @param type 识别类型
     * @return 识别结果
     */
    private ResponseEntity<Map<String, Object>> processFileRecognition(MultipartFile file, String type) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证文件
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请上传图片文件");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("success", false);
                result.put("message", "只支持图片格式（PNG、JPG、JPEG、BMP）");
                return ResponseEntity.badRequest().body(result);
            }

            // 验证文件大小，百度OCR限制为4MB
            if (file.getSize() > 4 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "图片大小不能超过4MB");
                return ResponseEntity.badRequest().body(result);
            }

            // 调用OCR服务
            String ocrResult;
            switch (type) {
                case "standard":
                    ocrResult = baiduOcrService.recognizeTextFileStandard(file);
                    break;
                case "standard-pos":
                    ocrResult = baiduOcrService.recognizeTextFileStandardWithPos(file);
                    break;
                case "high-accuracy":
                    ocrResult = baiduOcrService.recognizeTextFileHighAccuracy(file);
                    break;
                case "high-accuracy-pos":
                    ocrResult = baiduOcrService.recognizeTextFileHighAccuracyWithPos(file);
                    break;
                default:
                    ocrResult = baiduOcrService.recognizeTextFileStandard(file);
            }

            return buildSuccessResponse(ocrResult, type);

        } catch (Exception e) {
            log.error("图片识别异常", e);
            result.put("success", false);
            result.put("message", "识别异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 处理URL识别请求
     *
     * @param url  图片URL
     * @param type 识别类型
     * @return 识别结果
     */
    private ResponseEntity<Map<String, Object>> processUrlRecognition(String url, String type) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证URL
            if (url == null || url.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "请提供图片URL");
                return ResponseEntity.badRequest().body(result);
            }

            // 调用OCR服务
            String ocrResult;
            switch (type) {
                case "standard":
                    ocrResult = baiduOcrService.recognizeTextUrlStandard(url);
                    break;
                case "standard-pos":
                    ocrResult = baiduOcrService.recognizeTextUrlStandardWithPos(url);
                    break;
                case "high-accuracy":
                    ocrResult = baiduOcrService.recognizeTextUrlHighAccuracy(url);
                    break;
                case "high-accuracy-pos":
                    ocrResult = baiduOcrService.recognizeTextUrlHighAccuracyWithPos(url);
                    break;
                default:
                    ocrResult = baiduOcrService.recognizeTextUrlStandard(url);
            }

            return buildSuccessResponse(ocrResult, type);

        } catch (Exception e) {
            log.error("URL图片识别异常", e);
            result.put("success", false);
            result.put("message", "识别异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 构建成功响应
     *
     * @param ocrResult OCR原始结果
     * @param type      识别类型
     * @return 响应实体
     */
    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String ocrResult, String type) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 解析结果
        JsonNode jsonNode = objectMapper.readTree(ocrResult);

        // 检查是否有错误
        if (jsonNode.has("error_code")) {
            result.put("success", false);
            result.put("message", "识别失败: " + jsonNode.get("error_msg").asText());
            result.put("error_code", jsonNode.get("error_code").asInt());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }

        // 提取识别的文字
        List<String> words = new ArrayList<>();
        List<Map<String, Object>> wordsWithDetails = new ArrayList<>();

        JsonNode wordsResult = jsonNode.get("words_result");
        if (wordsResult != null && wordsResult.isArray()) {
            for (JsonNode wordNode : wordsResult) {
                String text = wordNode.get("words").asText();
                words.add(text);

                // 如果是含位置的版本，提取位置信息
                if (type.contains("-pos") && wordNode.has("location")) {
                    Map<String, Object> wordDetail = new HashMap<>();
                    wordDetail.put("words", text);

                    JsonNode location = wordNode.get("location");
                    Map<String, Integer> locationMap = new HashMap<>();
                    locationMap.put("left", location.get("left").asInt());
                    locationMap.put("top", location.get("top").asInt());
                    locationMap.put("width", location.get("width").asInt());
                    locationMap.put("height", location.get("height").asInt());
                    wordDetail.put("location", locationMap);

                    wordsWithDetails.add(wordDetail);
                }
            }
        }

        // 返回成功结果
        result.put("success", true);
        result.put("message", "识别成功");
        result.put("recognition_type", type);
        result.put("words_result_num", jsonNode.get("words_result_num").asInt());
        result.put("words", words);
        result.put("full_text", String.join("\n", words));

        if (type.contains("-pos")) {
            result.put("words_with_location", wordsWithDetails);
        }

        result.put("raw_result", ocrResult); // 原始结果，供调试使用

        log.info("图片识别成功，类型: {}, 识别到 {} 行文字", type, words.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "ok");
        result.put("service", "百度OCR服务");
        return ResponseEntity.ok(result);
    }
}