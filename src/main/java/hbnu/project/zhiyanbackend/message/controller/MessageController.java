package hbnu.project.zhiyanbackend.message.controller;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.message.model.converter.MessageConverter;
import hbnu.project.zhiyanbackend.message.model.dto.MessageListDTO;
import hbnu.project.zhiyanbackend.message.model.dto.SendCustomMessageToProjectDTO;
import hbnu.project.zhiyanbackend.message.model.dto.SendCustomMessageToUserDTO;
import hbnu.project.zhiyanbackend.message.model.entity.MessageRecipient;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 站内消息 Controller
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/message")
@RequiredArgsConstructor
@Tag(name = "站内消息管理", description = "站内消息查询、发送等接口")
public class MessageController {

    private final InboxMessageService inboxMessageService;

    private final UserRepository userRepository;

    private final ProjectMemberService projectMemberService;

    private final ProjectRepository projectRepository;

    /**
     * 获取消息列表
     * Repository 层已使用 JOIN FETCH 立即加载 messageBody，无需手动触发懒加载
     */
    @GetMapping("/list")
    public R<Page<MessageListDTO>> getMessageList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = SecurityUtils.getUserId();
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "triggerTime"));

            Page<MessageRecipient> recipientPage = inboxMessageService.pageInbox(userId, pageable);
            
            // 使用 MapStruct 转换，JOIN FETCH 已确保 messageBody 被加载
            Page<MessageListDTO> dtoPage = recipientPage.map(MessageConverter.INSTANCE::toListDTO);

            log.debug("获取消息列表成功: userId={}, page={}, size={}, total={}", 
                    userId, page, size, dtoPage.getTotalElements());
            return R.ok(dtoPage);
        } catch (Exception e) {
            log.error("获取消息列表失败: userId={}, page={}, size={}", 
                    SecurityUtils.getUserId(), page, size, e);
            throw e;
        }
    }

    /**
     * 获取未读消息列表（分页）
     * Repository 层已使用 JOIN FETCH 立即加载 messageBody，无需手动触发懒加载
     */
    @GetMapping("/unread")
    public R<Page<MessageListDTO>> getUnreadMessageList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Long userId = SecurityUtils.getUserId();
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "triggerTime"));

            Page<MessageRecipient> recipientPage = inboxMessageService.pageUnread(userId, pageable);
            
            // 使用 MapStruct 转换，JOIN FETCH 已确保 messageBody 被加载
            Page<MessageListDTO> dtoPage = recipientPage.map(MessageConverter.INSTANCE::toListDTO);

            log.debug("获取未读消息列表成功: userId={}, page={}, size={}, total={}", 
                    userId, page, size, dtoPage.getTotalElements());
            return R.ok(dtoPage);
        } catch (Exception e) {
            log.error("获取未读消息列表失败: userId={}, page={}, size={}", 
                    SecurityUtils.getUserId(), page, size, e);
            throw e;
        }
    }

    /**
     * 私信
     * 发送自定义消息给指定用户（通过用户名）
     * 权限要求：已登录
     */
    @PostMapping("/send/to-user")
    @Operation(summary = "发送消息给指定用户", description = "通过用户名向指定用户发送自定义消息")
    public R<Void> sendMessageToUser(@RequestBody @Valid SendCustomMessageToUserDTO dto) {
        try{
            Long senderId = SecurityUtils.getUserId();
            if (senderId == null) {
                return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
            }
            log.info("用户[{}]发送消息给用户[{}]", senderId, dto.getReceiverUsername());

            // 根据用户名查找接收者
            Long receiverId = userRepository.findByNameAndIsDeletedFalse(dto.getReceiverUsername())
                    .map(user -> user.getId())
                    .orElseThrow(() -> new ServiceException("用户不存在: " + dto.getReceiverUsername()));

            // 不能给自己发消息
            if (receiverId.equals(senderId)) {
                return R.fail("不能给自己发送消息");
            }

            // 获取发送者姓名
            String senderName = userRepository.findNameById(senderId)
                    .orElse("未知用户");

            // 构建扩展数据
            Map<String, Object> extendData = new HashMap<>();
            extendData.put("senderId", senderId);
            extendData.put("senderName", senderName);
            extendData.put("receiverId", receiverId);
            extendData.put("receiverUsername", dto.getReceiverUsername());
            String extendDataJson = JsonUtils.toJsonString(extendData);

            // 发送消息
            inboxMessageService.sendPersonalMessage(
                    MessageScene.USER_CUSTOM_MESSAGE,
                    senderId,
                    receiverId,
                    dto.getTitle(),
                    dto.getContent(),
                    null,
                    "USER",
                    extendDataJson
            );

            log.info("发送消息成功: senderId={}, receiverId={}", senderId, receiverId);
            return R.ok(null, "消息发送成功");
        } catch (ServiceException e) {
            log.error("发送消息失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发送消息异常", e);
            return R.fail("发送消息失败");
        }
    }

    /**
     * 群发自定义消息给项目成员（除了自己）
     * 权限要求：已登录 + 项目成员
     */
    @PostMapping("/send/to-project")
    @Operation(summary = "群发消息给项目成员", description = "向项目内除自己外的所有成员发送自定义消息")
    public R<Void> sendMessageToProject(@RequestBody @Valid SendCustomMessageToProjectDTO dto) {
        try {
            Long senderId = SecurityUtils.getUserId();
            if (senderId == null) {
                return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
            }
            log.info("用户[{}]群发消息给项目[{}]的成员", senderId, dto.getProjectId());

            // 检查是否为项目成员
            if (!projectMemberService.isMember(dto.getProjectId(), senderId)) {
                return R.fail("您不是该项目的成员，无权发送消息");
            }

            // 获取项目成员ID列表
            List<Long> projectMemberIds = projectMemberService.getProjectMemberUserIds(dto.getProjectId());

            if (projectMemberIds.isEmpty()) {
                return R.fail("项目中没有其他成员");
            }

            // 过滤掉发送者自己
            List<Long> receiverIds = projectMemberIds.stream()
                    .filter(userId -> !userId.equals(senderId))
                    .collect(Collectors.toList());

            if (receiverIds.isEmpty()) {
                return R.fail("项目中没有其他成员可接收消息");
            }

            // 获取发送者姓名
            String senderName = userRepository.findNameById(senderId)
                    .orElse("未知用户");

            // 获取项目名称
            String projectName = projectRepository.findProjectNameById(dto.getProjectId())
                    .orElse("未知项目");

            // 构建扩展数据
            Map<String, Object> extendData = new HashMap<>();
            extendData.put("senderId", senderId);
            extendData.put("senderName", senderName);
            extendData.put("projectId", dto.getProjectId());
            extendData.put("projectName", projectName);
            extendData.put("receiverCount", receiverIds.size());
            String extendDataJson = JsonUtils.toJsonString(extendData);

            // 发送批量消息
            inboxMessageService.sendBatchPersonalMessage(
                    MessageScene.USER_CUSTOM_MESSAGE,
                    senderId,
                    receiverIds,
                    dto.getTitle(),
                    dto.getContent(),
                    dto.getProjectId(),
                    "PROJECT",
                    extendDataJson
            );

            log.info("群发消息成功: senderId={}, projectId={}, receiverCount={}",
                    senderId, dto.getProjectId(), receiverIds.size());
            return R.ok(null, String.format("消息已发送给 %d 位项目成员", receiverIds.size()));
        }catch (Exception e) {
            log.error("群发消息异常", e);
            return R.fail("发送消息失败");
        }
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread/count")
    public R<Long> getUnreadCount() {
        Long userId = SecurityUtils.getUserId();
        long count = inboxMessageService.countUnread(userId);
        return R.ok(count);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{recipientId}")
    public R<Void> markAsRead(@PathVariable Long recipientId) {
        Long userId = SecurityUtils.getUserId();
        inboxMessageService.markAsRead(userId, recipientId);
        return R.ok();
    }

    /**
     * 全部标记为已读
     */
    @PutMapping("/read/all")
    public R<Void> markAllAsRead() {
        Long userId = SecurityUtils.getUserId();
        inboxMessageService.markAllAsRead(userId);
        return R.ok();
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{recipientId}")
    public R<Void> deleteMessage(@PathVariable Long recipientId) {
        Long userId = SecurityUtils.getUserId();
        inboxMessageService.deleteMessage(userId, recipientId);
        return R.ok();
    }

    /**
     * 清空所有消息
     */
    @DeleteMapping("/clear")
    public R<Void> clearAll() {
        Long userId = SecurityUtils.getUserId();
        inboxMessageService.clearAll(userId);
        return R.ok();
    }
}
