package hbnu.project.zhiyanbackend.ai.aiassistant.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.DifyAppInfoDTO;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.ConversationListResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.MessageListResponse;

public interface ConversationService {

    ConversationListResponse getConversations(Long userId, String lastId, Integer limit, String sortBy);

    boolean deleteConversation(String conversationId, Long userId);

    MessageListResponse getMessages(String conversationId, Long userId, String firstId, Integer limit);

    DifyAppInfoDTO getAppInfo();
}
