package hbnu.project.zhiyanbackend.message.controller;

import cn.hutool.core.lang.Dict;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.message.model.converter.MessageConverter;
import hbnu.project.zhiyanbackend.message.model.dto.*;
import hbnu.project.zhiyanbackend.message.model.entity.MessageRecipient;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.message.repository.MessageRecipientRepository;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final MessageRecipientRepository messageRecipientRepository;

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
     * 邀请用户加入项目
     * @param dto ProjectInviteMessageDTO
     * @return 发送结果
     */
    @PostMapping("/project/invite")
    @Operation(summary = "邀请用户加入项目")
    public R<Void> inviteUserToProject(@RequestBody @Valid ProjectInviteMessageDTO dto) {
        Long inviterId = SecurityUtils.getUserId();
        if (inviterId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        if (!projectMemberService.isAdmin(dto.getProjectId(), inviterId)) {
            return R.fail("只有项目管理员可以邀请成员");
        }
        if (projectMemberService.isMember(dto.getProjectId(), dto.getTargetUserId())) {
            return R.fail("该用户已是项目成员");
        }

        String inviterName = userRepository.findNameById(inviterId).orElse("未知用户");
        String receiverName = userRepository.findNameById(dto.getTargetUserId()).orElse("未知用户");
        String projectName = projectRepository.findProjectNameById(dto.getProjectId())
                .orElseThrow(() -> new ServiceException("项目不存在"));

        Map<String, Object> extend = new HashMap<>();
        extend.put("kind", "PROJECT_INVITATION");
        extend.put("projectId", dto.getProjectId());
        extend.put("projectName", projectName);
        extend.put("role", dto.getRole().name());
        extend.put("roleLabel", dto.getRole().getDescription());
        extend.put("inviterId", inviterId);
        extend.put("inviterName", inviterName);
        extend.put("targetUserId", dto.getTargetUserId());

        String content = Optional.ofNullable(dto.getMessage())
                .filter(StringUtils::hasText)
                .orElse(String.format("%s 邀请你作为「%s」加入项目「%s」",
                        inviterName, dto.getRole().getDescription(), projectName));

        inboxMessageService.sendPersonalMessage(
                MessageScene.PROJECT_MEMBER_INVITED,
                inviterId,
                dto.getTargetUserId(),
                "项目邀请：" + projectName,
                content,
                dto.getProjectId(),
                "PROJECT",
                JsonUtils.toJsonString(extend)
        );

        return R.ok(null, "邀请消息已发送");
    }

    /**
     * 用户申请加入项目
     * @param dto ProjectJoinApplyDTO
     * @return 申请发送结果
     */
    @PostMapping("/project/apply")
    @Operation(summary = "申请加入项目")
    public R<Void> applyToJoinProject(@RequestBody @Valid ProjectJoinApplyDTO dto) {
        Long applicantId = SecurityUtils.getUserId();
        if (applicantId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        if (projectMemberService.isMember(dto.getProjectId(), applicantId)) {
            return R.fail("您已经是项目成员");
        }

        List<Long> adminIds = projectMemberService.getProjectAdminUserIds(dto.getProjectId());
        if (adminIds.isEmpty()) {
            return R.fail("项目暂未设置管理员，无法提交申请，理论上这种情况不能发生");
        }

        String applicantName = userRepository.findNameById(applicantId).orElse("未知用户");
        String projectName = projectRepository.findProjectNameById(dto.getProjectId())
                .orElseThrow(() -> new ServiceException("项目不存在"));

        Map<String, Object> extend = new HashMap<>();
        extend.put("kind", "PROJECT_JOIN_APPLY");
        extend.put("projectId", dto.getProjectId());
        extend.put("projectName", projectName);
        extend.put("applicantId", applicantId);
        extend.put("applicantName", applicantName);
        extend.put("reason", dto.getReason());
        extend.put("defaultRole", ProjectMemberRole.MEMBER.name());

        String content = (StringUtils.hasText(dto.getReason()))
                ? String.format("%s 申请加入项目「%s」：%s", applicantName, projectName, dto.getReason())
                : String.format("%s 申请加入项目「%s」", applicantName, projectName);

        inboxMessageService.sendBatchPersonalMessage(
                MessageScene.PROJECT_MEMBER_APPLY,
                applicantId,
                adminIds,
                "新的项目加入申请",
                content,
                dto.getProjectId(),
                "PROJECT",
                JsonUtils.toJsonString(extend)
        );

        return R.ok(null, "申请已发送，等待管理员处理");
    }

    /**
     * 受邀用户同意加入项目
     */
    @PostMapping("/project/invite/{recipientId}/accept")
    @Operation(summary = "同意项目邀请")
    public R<Void> acceptProjectInvitation(@PathVariable Long recipientId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        // 校验消息归属及类型
        MessageRecipient recipient = messageRecipientRepository
                .findByIdAndReceiverIdAndDeletedFalse(recipientId, userId)
                .orElseThrow(() -> new ServiceException("邀请消息不存在或已失效"));

        if (!MessageScene.PROJECT_MEMBER_INVITED.name().equals(recipient.getSceneCode())) {
            return R.fail("该消息不是项目邀请，无法执行此操作");
        }

        // 解析扩展数据
        String extendJson = recipient.getMessageBody().getExtendData();
        Dict extend = JsonUtils.parseMap(extendJson);
        if (extend == null) {
            return R.fail("邀请消息数据异常，无法加入项目");
        }

        Long projectId = extend.getLong("projectId");
        Long inviterId = extend.getLong("inviterId");
        String projectName = extend.containsKey("projectName") ? extend.getStr("projectName") : "未知项目";
        String roleCode = extend.containsKey("role") ? extend.getStr("role") : ProjectMemberRole.MEMBER.name();

        ProjectMemberRole role = ProjectMemberRole.MEMBER;
        try {
            role = ProjectMemberRole.valueOf(roleCode);
        } catch (IllegalArgumentException ignored) {
        }

        // 调用原有成员加入逻辑
        R<Void> addResult = projectMemberService.addMember(projectId, userId, role);
        if (!R.isSuccess(addResult) && !"该用户已经是项目成员".equals(addResult.getMsg())) {
            return addResult;
        }

        // 将邀请消息标记为已读
        inboxMessageService.markAsRead(userId, recipientId);

        String inviteeName = userRepository.findNameById(userId).orElse("未知用户");

        // 给邀请人发送“已接受”通知
        if (inviterId != null) {
            Map<String, Object> approvalExtend = new HashMap<>();
            approvalExtend.put("kind", "PROJECT_INVITATION_ACCEPTED");
            approvalExtend.put("projectId", projectId);
            approvalExtend.put("projectName", projectName);
            approvalExtend.put("inviterId", inviterId);
            approvalExtend.put("inviteeId", userId);
            approvalExtend.put("inviteeName", inviteeName);

            inboxMessageService.sendPersonalMessage(
                    MessageScene.PROJECT_MEMBER_APPROVAL,
                    userId,
                    inviterId,
                    "项目邀请已被接受",
                    String.format("%s 已接受你对项目「%s」的邀请", inviteeName, projectName),
                    projectId,
                    "PROJECT",
                    JsonUtils.toJsonString(approvalExtend)
            );
        }

        return R.ok(null, "已成功加入项目");
    }


    /**
     * 受邀用户拒绝加入项目
     */
    @PostMapping("/project/invite/{recipientId}/reject")
    @Operation(summary = "拒绝项目邀请")
    public R<Void> rejectProjectInvitation(@PathVariable Long recipientId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        MessageRecipient recipient = messageRecipientRepository
                .findByIdAndReceiverIdAndDeletedFalse(recipientId, userId)
                .orElseThrow(() -> new ServiceException("邀请消息不存在或已失效"));

        if (!MessageScene.PROJECT_MEMBER_INVITED.name().equals(recipient.getSceneCode())) {
            return R.fail("该消息不是项目邀请，无法执行此操作");
        }

        String extendJson = recipient.getMessageBody().getExtendData();
        Dict extend = JsonUtils.parseMap(extendJson);
        if (extend == null) {
            return R.fail("邀请消息数据异常，无法处理");
        }

        Long projectId = extend.getLong("projectId");
        Long inviterId = extend.getLong("inviterId");
        String projectName = extend.containsKey("projectName") ? extend.getStr("projectName") : "未知项目";
        String inviterName = extend.containsKey("inviterName") ? extend.getStr("inviterName") : "邀请人";

        // 标记原消息为已读
        inboxMessageService.markAsRead(userId, recipientId);

        String inviteeName = userRepository.findNameById(userId).orElse("未知用户");

        if (inviterId != null) {
            Map<String, Object> rejectExtend = new HashMap<>();
            rejectExtend.put("kind", "PROJECT_INVITATION_REJECTED");
            rejectExtend.put("projectId", projectId);
            rejectExtend.put("projectName", projectName);
            rejectExtend.put("inviterId", inviterId);
            rejectExtend.put("inviterName", inviterName);
            rejectExtend.put("inviteeId", userId);
            rejectExtend.put("inviteeName", inviteeName);

            inboxMessageService.sendPersonalMessage(
                    MessageScene.PROJECT_MEMBER_APPROVAL,
                    userId,
                    inviterId,
                    "项目邀请已被拒绝",
                    String.format("%s 拒绝了你对项目「%s」的邀请", inviteeName, projectName),
                    projectId,
                    "PROJECT",
                    JsonUtils.toJsonString(rejectExtend)
            );
        }

        return R.ok(null, "已拒绝项目邀请");
    }

    /**
     * 管理员同意项目加入申请
     */
    @PostMapping("/project/apply/{recipientId}/approve")
    @Operation(summary = "同意项目加入申请")
    public R<Void> approveProjectJoin(@PathVariable Long recipientId) {
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        MessageRecipient recipient = messageRecipientRepository
                .findByIdAndReceiverIdAndDeletedFalse(recipientId, operatorId)
                .orElseThrow(() -> new ServiceException("申请消息不存在或已失效"));

        if (!MessageScene.PROJECT_MEMBER_APPLY.name().equals(recipient.getSceneCode())) {
            return R.fail("该消息不是加入申请，无法执行此操作");
        }

        String extendJson = recipient.getMessageBody().getExtendData();
        Dict extend = JsonUtils.parseMap(extendJson);
        if (extend == null) {
            return R.fail("申请消息数据异常，无法处理");
        }

        Long projectId = extend.getLong("projectId");
        Long applicantId = extend.getLong("applicantId");
        String projectName = extend.containsKey("projectName") ? extend.getStr("projectName") : "未知项目";
        String defaultRole = extend.containsKey("defaultRole") ? extend.getStr("defaultRole") : ProjectMemberRole.MEMBER.name();

        ProjectMemberRole role = ProjectMemberRole.MEMBER;
        try {
            role = ProjectMemberRole.valueOf(defaultRole);
        } catch (IllegalArgumentException ignored) {
        }

        // 管理员校验
        if (!projectMemberService.isAdmin(projectId, operatorId)) {
            return R.fail("只有项目管理员可以处理加入申请");
        }

        R<Void> addResult = projectMemberService.addMember(projectId, applicantId, role);
        if (!R.isSuccess(addResult) && !"该用户已经是项目成员".equals(addResult.getMsg())) {
            return addResult;
        }

        // 标记申请消息为已读
        inboxMessageService.markAsRead(operatorId, recipientId);

        String applicantName = extend.containsKey("applicantName") ? extend.getStr("applicantName") : "申请人";
        String operatorName = userRepository.findNameById(operatorId).orElse("管理员");

        Map<String, Object> approvalExtend = new HashMap<>();
        approvalExtend.put("kind", "PROJECT_JOIN_APPROVED");
        approvalExtend.put("projectId", projectId);
        approvalExtend.put("projectName", projectName);
        approvalExtend.put("applicantId", applicantId);
        approvalExtend.put("applicantName", applicantName);
        approvalExtend.put("operatorId", operatorId);
        approvalExtend.put("operatorName", operatorName);

        // 给申请人发送审批结果
        inboxMessageService.sendPersonalMessage(
                MessageScene.PROJECT_MEMBER_APPROVAL,
                operatorId,
                applicantId,
                "项目加入申请已通过",
                String.format("您的项目「%s」加入申请已通过，审批人：%s", projectName, operatorName),
                projectId,
                "PROJECT",
                JsonUtils.toJsonString(approvalExtend)
        );

        return R.ok(null, "已同意加入申请");
    }

    /**
     * 管理员拒绝项目加入申请
     */
    @PostMapping("/project/apply/{recipientId}/reject")
    @Operation(summary = "拒绝项目加入申请")
    public R<Void> rejectProjectJoin(@PathVariable Long recipientId) {
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        MessageRecipient recipient = messageRecipientRepository
                .findByIdAndReceiverIdAndDeletedFalse(recipientId, operatorId)
                .orElseThrow(() -> new ServiceException("申请消息不存在或已失效"));

        if (!MessageScene.PROJECT_MEMBER_APPLY.name().equals(recipient.getSceneCode())) {
            return R.fail("该消息不是加入申请，无法执行此操作");
        }

        String extendJson = recipient.getMessageBody().getExtendData();
        Dict extend = JsonUtils.parseMap(extendJson);
        if (extend == null) {
            return R.fail("申请消息数据异常，无法处理");
        }

        Long projectId = extend.getLong("projectId");
        Long applicantId = extend.getLong("applicantId");
        String projectName = extend.containsKey("projectName") ? extend.getStr("projectName") : "未知项目";
        String applicantName = extend.containsKey("applicantName") ? extend.getStr("applicantName") : "申请人";

        // 管理员校验
        if (!projectMemberService.isAdmin(projectId, operatorId)) {
            return R.fail("只有项目管理员可以处理加入申请");
        }

        // 标记申请消息为已读
        inboxMessageService.markAsRead(operatorId, recipientId);

        String operatorName = userRepository.findNameById(operatorId).orElse("管理员");

        Map<String, Object> rejectExtend = new HashMap<>();
        rejectExtend.put("kind", "PROJECT_JOIN_REJECTED");
        rejectExtend.put("projectId", projectId);
        rejectExtend.put("projectName", projectName);
        rejectExtend.put("applicantId", applicantId);
        rejectExtend.put("applicantName", applicantName);
        rejectExtend.put("operatorId", operatorId);
        rejectExtend.put("operatorName", operatorName);

        inboxMessageService.sendPersonalMessage(
                MessageScene.PROJECT_MEMBER_APPROVAL,
                operatorId,
                applicantId,
                "项目加入申请未通过",
                String.format("您加入项目「%s」的申请已被拒绝，处理人：%s", projectName, operatorName),
                projectId,
                "PROJECT",
                JsonUtils.toJsonString(rejectExtend)
        );

        return R.ok(null, "已拒绝加入申请");
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
