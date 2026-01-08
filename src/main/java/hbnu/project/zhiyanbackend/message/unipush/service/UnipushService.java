package hbnu.project.zhiyanbackend.message.unipush.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.message.unipush.dto.PushMessageRequest;
import hbnu.project.zhiyanbackend.message.unipush.dto.PushMessageResponse;
import hbnu.project.zhiyanbackend.message.unipush.config.UnipushConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Unipush消息推送服务
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnipushService {

    private final UnipushConfig unipushConfig;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 发送消息
     *
     * @param clientIds 死吗cid
     * @param title   cnm标题
     * @param content  cjb内容
     * @param payload  数据
     * @return PushMessageResponse
     */
    public PushMessageResponse sendMessage(List<String> clientIds, String title, String content, Map<String, Object> payload) {
        PushMessageRequest request = PushMessageRequest.builder()
                .pushClientId(clientIds)
                .title(title)
                .content(content)
                .payload(payload)
                .requestId(generateRequestId())
                .build();

        return sendMessage(request);
    }


    /**
     * 发送推送消息
     *
     * @param request 推送请求对象
     * @return 推送结果
     */
    public PushMessageResponse sendMessage(PushMessageRequest request) {
        try{
            // 验证必填参数
            validateRequest(request);

            // 如果没有设置requestId,这个需要自动生成
            // 请求唯一标识号，10-32位之间；如果request_id重复，会导致消息丢失
            if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
                request.setRequestId(generateRequestId());
            }

            // 构建请求体,匹配云函数的格式
            Map<String, Object> requestBody = buildCloudFunctionRequest(request);

            log.info("开始发送推送消息, clientIds: {}, title: {}", request.getPushClientId(), request.getTitle());

            // 发送HTTP请求到云函数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<PushMessageResponse> response = restTemplate.exchange(
                    unipushConfig.getPushUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    PushMessageResponse.class
            );

            PushMessageResponse result = response.getBody();

            if (result != null && result.getErrCode() == 0) {
                log.info("推送消息成功, taskId: {}", result.getTaskId());
            } else {
                log.error("推送消息失败, errCode: {}, errMsg: {}",
                        result != null ? result.getErrCode() : "null",
                        result != null ? result.getErrMsg() : "null");
            }

            return result;
        }catch (ServiceException e){
            log.error("推送消息异常", e);

            // 返回错误响应
            PushMessageResponse errorResponse = new PushMessageResponse();
            errorResponse.setErrCode(-1);
            errorResponse.setErrMsg("推送失败: " + e.getMessage());
            return errorResponse;
        }
    }


    /**
     * cid单推
     * 其实更多是测试用
     *
     * @param clientId 客户端ID
     * @param title 标题
     * @param content 内容
     * @param payload 自定义数据
     * @return 推送结果
     */
    public PushMessageResponse sendToSingleUser(String clientId, String title, String content, Map<String, Object> payload) {
        return sendMessage(Collections.singletonList(clientId), title, content, payload);
    }


    /**
     * 批量推送
     * 自动分批，因为unipush一次最多只能同时给1000个用户发
     * 虽然平台没有那么多的一次发送消息的需求，但是为了对齐，还是需要这么写一下的
     *
     * @param clientIds   客户端id列表
     * @param title       标题
     * @param content     内容
     * @param payload     自定义数据
     * @return   推送消息响应列表
     */
    public List<PushMessageResponse> batchSendMessage(List<String> clientIds, String title, String content, Map<String, Object> payload) {
        List<PushMessageResponse> results = new ArrayList<>();

        int batchSize = 1000;
        for(int i = 0; i < clientIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, clientIds.size());
            List<String> batchClientIds = clientIds.subList(i, endIndex);

            PushMessageResponse response = sendMessage(batchClientIds, title, content, payload);
            results.add(response);

            // 批次间延迟1s,避免频繁请求炸缸
            if (endIndex < clientIds.size()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return results;
    }

    // --------------------------------------------------------------------------------------

    /**
     * 构建DCloud消息推送unipush云函数请求体
     * @param request  原生PushMessageRequest dto
     * @return unipush用的云函数请求体
     */
    private Map<String, Object> buildCloudFunctionRequest(PushMessageRequest request) {
        Map<String, Object> body = new HashMap<>();

        // 基础字段，这些字段得有，payload不确定，先不写
        body.put("cids", request.getPushClientId());
        body.put("title", request.getTitle());
        body.put("content", request.getContent());
        body.put("request_id", request.getRequestId());
        // payload数据
        if (request.getPayload() != null && !request.getPayload().isEmpty()) {
            body.put("payload", request.getPayload());
        }

        // 消息有效期设置(默认2小时)
        Map<String, Object> settings = new HashMap<>();
        settings.put("ttl", 72000000);   // 20小时,单位毫秒
        body.put("settings", settings);

        return body;
    }


    /**
     * 验证请求参数
     */
    private void validateRequest(PushMessageRequest request) {
        if (request.getPushClientId() == null || request.getPushClientId().isEmpty()) {
            throw new IllegalArgumentException("客户端ID列表不能为空");
        }

        if (request.getPushClientId().size() > 1000) {
            throw new IllegalArgumentException("单次推送客户端ID数量不能超过1000");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("推送标题不能为空");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("推送内容不能为空");
        }

        // 验证payload大小(不超过800字符)
        if (request.getPayload() != null) {
            try {
                String payloadJson = objectMapper.writeValueAsString(request.getPayload());
                if (payloadJson.length() > 800) {
                    throw new IllegalArgumentException("payload数据长度不能超过800字符");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("payload数据格式错误");
            }
        }
    }

    /**
     * 生成唯一的请求ID(10-32位)
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
