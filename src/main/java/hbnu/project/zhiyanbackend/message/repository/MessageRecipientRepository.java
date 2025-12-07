package hbnu.project.zhiyanbackend.message.repository;


import hbnu.project.zhiyanbackend.message.model.entity.MessageRecipient;
import hbnu.project.zhiyanbackend.message.model.enums.MessagePriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息收件人repository
 *
 * @author ErgouTree
 */
@Repository
public interface MessageRecipientRepository extends JpaRepository<MessageRecipient, Long> {

    /**
     * 用户收件箱分页查询（排除已删除）
     * 使用 JOIN FETCH 立即加载 messageBody 关联，避免 LazyInitializationException
     */
    @Query(value = "SELECT DISTINCT mr FROM MessageRecipient mr " +
           "JOIN FETCH mr.messageBody mb " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mr.deleted = false " +
           "ORDER BY mr.triggerTime DESC",
           countQuery = "SELECT COUNT(DISTINCT mr) FROM MessageRecipient mr " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mr.deleted = false")
    Page<MessageRecipient> findByReceiverIdAndDeletedFalseOrderByTriggerTimeDesc(
            @Param("receiverId") Long receiverId, 
            Pageable pageable);

    /**
     * 用户未读消息分页查询
     * 使用 JOIN FETCH 立即加载 messageBody 关联，避免 LazyInitializationException
     */
    @Query(value = "SELECT DISTINCT mr FROM MessageRecipient mr " +
           "JOIN FETCH mr.messageBody mb " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mr.readFlag = false " +
           "AND mr.deleted = false " +
           "ORDER BY mr.triggerTime DESC",
           countQuery = "SELECT COUNT(DISTINCT mr) FROM MessageRecipient mr " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mr.readFlag = false " +
           "AND mr.deleted = false")
    Page<MessageRecipient> findByReceiverIdAndReadFlagFalseAndDeletedFalseOrderByTriggerTimeDesc(
            @Param("receiverId") Long receiverId, 
            Pageable pageable);

    /**
     * 未读数量统计
     */
    long countByReceiverIdAndReadFlagFalseAndDeletedFalse(Long receiverId);

    /**
     * 根据 ID + 用户校验一条消息（防止越权）
     * 使用 JOIN FETCH 立即加载 messageBody 关联，避免 LazyInitializationException
     */
    @Query("SELECT mr FROM MessageRecipient mr " +
           "JOIN FETCH mr.messageBody mb " +
           "WHERE mr.id = :id " +
           "AND mr.receiverId = :receiverId " +
           "AND mr.deleted = false")
    Optional<MessageRecipient> findByIdAndReceiverIdAndDeletedFalse(@Param("id") Long id, @Param("receiverId") Long receiverId);

    /**
     * 查询用户全部未读，用于全部已读
     */
    List<MessageRecipient> findByReceiverIdAndReadFlagFalseAndDeletedFalse(Long receiverId);

    /**
     * 查询用户全部未删除，用于“清空消息”
     */
    List<MessageRecipient> findByReceiverIdAndDeletedFalse(Long receiverId);

    /**
     * 按时间范围查询,用于多久清理任务
     */
    List<MessageRecipient> findByReceiverIdAndDeletedFalseAndTriggerTimeBefore(Long receiverId, LocalDateTime time);

    /**
     * 按场景筛选消息
     * 使用 JOIN FETCH 立即加载 messageBody 关联，避免 LazyInitializationException
     */
    @Query(value = "SELECT mr FROM MessageRecipient mr " +
           "JOIN FETCH mr.messageBody mb " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mr.sceneCode = :sceneCode " +
           "AND mr.deleted = false " +
           "ORDER BY mr.triggerTime DESC",
           countQuery = "SELECT COUNT(mr) FROM MessageRecipient mr " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mr.sceneCode = :sceneCode " +
           "AND mr.deleted = false")
    Page<MessageRecipient> findByReceiverIdAndSceneCodeAndDeletedFalseOrderByTriggerTimeDesc(
            @Param("receiverId") Long receiverId, 
            @Param("sceneCode") String sceneCode, 
            Pageable pageable);

    /**
     * 按优先级筛选消息
     * 使用 JOIN FETCH 立即加载 messageBody 关联，避免 LazyInitializationException
     */
    @Query(value = "SELECT mr FROM MessageRecipient mr " +
           "JOIN FETCH mr.messageBody mb " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mb.priority = :priority " +
           "AND mr.deleted = false " +
           "ORDER BY mr.triggerTime DESC",
           countQuery = "SELECT COUNT(mr) FROM MessageRecipient mr " +
           "JOIN mr.messageBody mb " +
           "WHERE mr.receiverId = :receiverId " +
           "AND mb.priority = :priority " +
           "AND mr.deleted = false")
    Page<MessageRecipient> findByReceiverIdAndMessageBody_PriorityAndDeletedFalseOrderByTriggerTimeDesc(
            @Param("receiverId") Long receiverId, 
            @Param("priority") MessagePriority priority,
            Pageable pageable);

    /**
     * 搜索消息（按标题或内容）
     * 使用 JOIN FETCH 立即加载 messageBody 关联，避免 LazyInitializationException
     */
    @Query(value = "SELECT mr FROM MessageRecipient mr " +
            "JOIN FETCH mr.messageBody mb " +
            "WHERE mr.receiverId = :receiverId " +
            "AND mr.deleted = false " +
            "AND (mb.title LIKE CONCAT('%', :keyword, '%') OR mb.content LIKE CONCAT('%', :keyword, '%')) " +
            "ORDER BY mr.triggerTime DESC",
            countQuery = "SELECT COUNT(mr) FROM MessageRecipient mr " +
            "JOIN mr.messageBody mb " +
            "WHERE mr.receiverId = :receiverId " +
            "AND mr.deleted = false " +
            "AND (mb.title LIKE CONCAT('%', :keyword, '%') OR mb.content LIKE CONCAT('%', :keyword, '%'))")
    Page<MessageRecipient> searchMessage(@Param("receiverId") Long receiverId,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    /**
     * 查找指定场景下过期的未读消息
     * 用于定时任务标记过期消息
     *
     * @param sceneCode 场景代码
     * @param expireTime 过期时间点(3天前)
     * @return 过期的消息列表
     */
    @Query("SELECT mr FROM MessageRecipient mr " +
            "JOIN FETCH mr.messageBody mb " +
            "WHERE mr.sceneCode = :sceneCode " +
            "AND mr.readFlag = false " +
            "AND mr.deleted = false " +
            "AND mr.expired = false " +
            "AND mr.triggerTime < :expireTime")
    List<MessageRecipient> findExpiredUnreadMessages(
            @Param("sceneCode") String sceneCode,
            @Param("expireTime") LocalDateTime expireTime);

    /**
     * 查询用户全部已读且未删除的消息，用于清理全部已读消息
     */
    List<MessageRecipient> findByReceiverIdAndReadFlagTrueAndDeletedFalse(Long receiverId);

    /**
     * 统计某个消息体的收件人数量（用于判断是否删除消息体）
     */
    long countByMessageBodyId(Long messageBodyId);
}
