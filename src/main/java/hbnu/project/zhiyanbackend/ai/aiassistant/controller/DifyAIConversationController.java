package hbnu.project.zhiyanbackend.ai.aiassistant.controller;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.dto.DifyAppInfoDTO;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.ConversationListResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.MessageListResponse;
import hbnu.project.zhiyanbackend.ai.aiassistant.service.ConversationService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/zhiyan/ai/dify/conversations")
@RequiredArgsConstructor
@Tag(name = "AI 对话历史", description = "AI 对话历史管理接口，用于获取和管理对话记录")
public class DifyAIConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @Operation(summary = "获取会话列表", description = "获取当前用户的会话列表，默认返回最近 20 条。支持分页：传递 lastId 获取下一页数据。")
    public R<ConversationListResponse> getConversation(
            @Parameter(description = "当前页最后一条记录的 ID，用于分页") @RequestParam(required = false) String lastId,
            @Parameter(description = "返回记录数量，默认 20，最大 100") @RequestParam(required = false, defaultValue = "20") Integer limit,
            @Parameter(description = "排序字段，默认 -updated_at（按更新时间倒序）") @RequestParam(required = false, defaultValue = "-updated_at") String sortBy
    ) {
        Long userId = SecurityUtils.getUserId();
        log.info("[获取会话列表] userId={}, lastId={}, limit={}, sortBy={}", userId, lastId, limit, sortBy);

        ConversationListResponse response = conversationService.getConversations(userId, lastId, limit, sortBy);
        return R.ok(response, "获取会话列表成功");
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "获取会话历史消息", description = "获取指定会话的历史消息列表，支持分页。传递 firstId 获取更早的消息记录。")
    public R<MessageListResponse> getMessages(
            @Parameter(description = "会话 ID") @PathVariable String conversationId,
            @Parameter(description = "当前页第一条消息的 ID，用于分页获取更早的消息") @RequestParam(required = false) String firstId,
            @Parameter(description = "返回记录数量，默认 20") @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        Long userId = SecurityUtils.getUserId();
        log.info("[获取会话消息] conversationId={}, userId={}, firstId={}, limit={}", conversationId, userId, firstId, limit);

        MessageListResponse response = conversationService.getMessages(conversationId, userId, firstId, limit);
        return R.ok(response, "获取会话消息成功");
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "删除会话", description = "删除指定的会话及其所有历史消息")
    public R<Boolean> deleteConversation(
            @Parameter(description = "会话 ID") @PathVariable String conversationId
    ) {
        Long userId = SecurityUtils.getUserId();
        log.info("[删除会话] conversationId={}, userId={}", conversationId, userId);

        boolean success = conversationService.deleteConversation(conversationId, userId);
        return success ? R.ok(true, "会话删除成功") : R.fail("会话删除失败");
    }

    @GetMapping("/chatflow-info")
    @Operation(summary = "获取应用基本信息", description = "获取当前 Dify 应用的基本信息，包括名称、描述、标签、模式等")
    public R<DifyAppInfoDTO> getAppInfo() {
        log.info("[获取应用信息] 开始请求");
        DifyAppInfoDTO appInfo = conversationService.getAppInfo();
        return R.ok(appInfo, "获取应用信息成功");
    }
}
