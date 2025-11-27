package hbnu.project.zhiyanbackend.message.controller;


import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.message.model.converter.MessageConverter;
import hbnu.project.zhiyanbackend.message.model.dto.MessageListDTO;
import hbnu.project.zhiyanbackend.message.model.entity.MessageRecipient;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 站内消息 Controller
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/message")
@RequiredArgsConstructor
public class MessageController {

    private final InboxMessageService inboxMessageService;


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
