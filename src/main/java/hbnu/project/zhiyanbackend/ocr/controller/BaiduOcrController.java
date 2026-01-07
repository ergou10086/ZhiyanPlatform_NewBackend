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
        "http://zyplatform.xyz",       // HTTP生产环境
        "https://zyplatform.xyz",      // HTTPS生产环境
        "http://api.zyplatform.xyz",   // API域名
        "https://api.zyplatform.xyz"   // API域名
})
@RequiredArgsConstructor
public class BaiduOcrController {

    private final BaiduOcrService baiduOcrService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传图片进行文字识别
     * 调用 通用文字识别标准版 接口
     * 该接口传入图片进行文字识别
     *
     * @param file 图片文件
     * @return 识别结果
     */
    @PostMapping("/img-recognize/load/standard")
    public ResponseEntity<Map<String, Object>> recognizeImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try{
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

            // 验证文件大小，百度OCR限制为8MB
            // 图像数据，base64编码后进行urlencode，要求base64编码和urlencode后大小不超过8M
            if (file.getSize() > 8 * 1024 * 1024) {
                result.put("success", false);
                result.put("message", "图片大小不能超过8MB");
                return ResponseEntity.badRequest().body(result);
            }

            // 调用OCR服务
            String ocrResult = baiduOcrService.recognizeText(file);

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
            // 返回示例
            /*
            {
                "log_id": 2471272194,
                "words_result_num": 2,
                "words_result":
	                [
		                {"words": " TSINGTAO"},
		                {"words": "青岛啤酒"}
	                ]
            }
            */
            List<String> words = new ArrayList<>();
            JsonNode wordsResult = jsonNode.get("words_result");
            if (wordsResult != null && wordsResult.isArray()) {
                for (JsonNode wordNode : wordsResult) {
                    words.add(wordNode.get("words").asText());
                }
            }

            // 返回成功结果
            result.put("success", true);
            result.put("message", "识别成功");
            result.put("words_result_num", jsonNode.get("words_result_num").asInt());
            result.put("words", words);
            result.put("full_text", String.join("\n", words));
            result.put("raw_result", ocrResult); // 原始结果，供调试使用

            log.info("图片识别成功，识别到 {} 行文字", words.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("图片识别异常", e);
            result.put("success", false);
            result.put("message", "识别异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
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
