package hbnu.project.zhiyanbackend.ai.aiassistant.service.impl;

import hbnu.project.zhiyanbackend.ai.aiassistant.config.DifyProperties;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.DifyAppInfoDTO;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.ConversationListResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.MessageListResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final DifyProperties difyProperties;
    private final RestTemplate restTemplate;

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(difyProperties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Override
    public ConversationListResponse getConversations(Long userId, String lastId, Integer limit, String sortBy) {
        try {
            String userIdentifier = String.valueOf(userId);
            String baseUrl = difyProperties.getApiUrl() + "/conversations";

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("user", userIdentifier);

            if (lastId != null && !lastId.isBlank()) {
                builder.queryParam("last_id", lastId);
            }
            if (limit != null) {
                builder.queryParam("limit", limit);
            }
            if (sortBy != null && !sortBy.isBlank()) {
                builder.queryParam("sort_by", sortBy);
            }

            URI uri = builder.build(true).toUri();

            HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());

            ResponseEntity<ConversationListResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    ConversationListResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("[Dify 会话列表] 获取失败, userId={}, error={}", userId, e.getMessage(), e);
            throw new RuntimeException("获取会话列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteConversation(String conversationId, Long userId) {
        try {
            String baseUrl = difyProperties.getApiUrl() + "/conversations/" + conversationId;

            HttpHeaders headers = buildAuthHeaders();
            String userIdentifier = String.valueOf(userId);
            String bodyJson = "{\"user\":\"" + userIdentifier + "\"}";

            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            // Dify 删除成功通常返回 200/204
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("[Dify 删除会话] 失败, conversationId={}, userId={}, error={}", conversationId, userId, e.getMessage(), e);
            throw new RuntimeException("删除会话失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MessageListResponse getMessages(String conversationId, Long userId, String firstId, Integer limit) {
        try {
            String userIdentifier = String.valueOf(userId);
            String baseUrl = difyProperties.getApiUrl() + "/messages";

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("conversation_id", conversationId)
                    .queryParam("user", userIdentifier);

            if (firstId != null && !firstId.isBlank()) {
                builder.queryParam("first_id", firstId);
            }
            if (limit != null) {
                builder.queryParam("limit", limit);
            }

            URI uri = builder.build(true).toUri();

            HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());

            ResponseEntity<MessageListResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    MessageListResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("[Dify 会话消息] 获取失败, conversationId={}, userId={}, error={}", conversationId, userId, e.getMessage(), e);
            throw new RuntimeException("获取会话消息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public DifyAppInfoDTO getAppInfo() {
        try {
            String baseUrl = difyProperties.getApiUrl() + "/info";

            HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders());

            ResponseEntity<DifyAppInfoDTO> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.GET,
                    entity,
                    DifyAppInfoDTO.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("[Dify 应用信息] 获取失败, error={}", e.getMessage(), e);
            throw new RuntimeException("获取应用信息失败: " + e.getMessage(), e);
        }
    }
}
