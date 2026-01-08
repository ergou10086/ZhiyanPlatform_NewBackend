package hbnu.project.zhiyanbackend.message.unipush.controller;

import hbnu.project.zhiyanbackend.message.unipush.dto.PushMessageRequest;
import hbnu.project.zhiyanbackend.message.unipush.dto.PushMessageResponse;
import hbnu.project.zhiyanbackend.message.unipush.service.UnipushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Unipush消息推送控制器
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/message/unipush")
@RequiredArgsConstructor
@Tag(name = "unipush消息推送接口", description = "向DCloud中的云函数发送请求调用其消息推送")
public class UnipushController {

    private final UnipushService unipushService;

    /**
     * 发送推送消息
     * 带payload载荷
     */
    @PostMapping("/send")
    @Operation(summary = "发送推送消息", description = "发送Unipush推送消息")
    public ResponseEntity<PushMessageResponse> sendMessage(@RequestBody PushMessageRequest request) {
        log.info("接收推送请求: clientIds={}, title={}", request.getPushClientId(), request.getTitle());
        PushMessageResponse response = unipushService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 发送推送消息
     * 不带payload载荷
     */
    @PostMapping("/send/simple")
    @Operation(summary = "发送简单推送", description = "发送简单的推送消息，仅标题和内容")
    public ResponseEntity<PushMessageResponse> sendSimpleMessage(
            @RequestParam List<String> clientIds,
            @RequestParam String title,
            @RequestParam String content) {

        log.info("接收简单推送请求: clientIds={}, title={}", clientIds, title);
        PushMessageResponse response = unipushService.sendMessage(clientIds, title, content, null);
        return ResponseEntity.ok(response);
    }

    /**
     * cid单推
     */
    @PostMapping("/send/single")
    @Operation(summary = "单用户推送", description = "向单个用户发送推送消息")
    public ResponseEntity<PushMessageResponse> sendToSingleUser(
            @RequestParam String clientId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestBody(required = false) Map<String, Object> payload) {

        log.info("接收单用户推送请求: clientId={}, title={}", clientId, title);
        PushMessageResponse response = unipushService.sendToSingleUser(clientId, title, content, payload);
        return ResponseEntity.ok(response);
    }

    /**
     * 批量推送
     */
    @PostMapping("/send/batch")
    @Operation(summary = "批量推送", description = "批量发送推送消息(自动分批处理)")
    public ResponseEntity<List<PushMessageResponse>> batchSendMessage(
            @RequestParam List<String> clientIds,
            @RequestParam String title,
            @RequestParam String content,
            @RequestBody(required = false) Map<String, Object> payload) {

        log.info("接收批量推送请求: clientIds数量={}, title={}", clientIds.size(), title);
        List<PushMessageResponse> responses = unipushService.batchSendMessage(clientIds, title, content, payload);
        return ResponseEntity.ok(responses);
    }

    /**
     * 测试推送(用于测试消息推送功能是否正常)
     */
    @PostMapping("/test")
    @Operation(summary = "测试推送", description = "发送测试推送消息")
    public ResponseEntity<PushMessageResponse> testPush(@RequestParam String clientId) {
        log.info("接收测试推送请求: clientId={}", clientId);

        String title = "测试通知";
        String content = "这是一条测试推送消息,时间: " + System.currentTimeMillis();

        PushMessageResponse response = unipushService.sendToSingleUser(clientId, title, content, null);
        return ResponseEntity.ok(response);
    }
}
