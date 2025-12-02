package hbnu.project.zhiyanbackend.wiki.service;

import hbnu.project.zhiyanbackend.wiki.model.dto.WikiCollaborationDTO;

import java.util.List;

/**
 * Wiki 协同编辑服务
 *
 * <p>负责管理在线编辑者状态、光标位置同步、内容锁控制，支撑多用户同时编辑Wiki页面的协同能力，
 * 避免编辑冲突，提升协作效率。</p>
 */
public interface WikiCollaborationService {

    /**
     * 用户加入Wiki页面编辑
     *
     * @param pageId Wiki页面ID（用户要编辑的页面）
     * @param userId 用户ID（加入编辑的用户）
     */
    void joinEditing(Long pageId, Long userId);

    /**
     * 用户离开编辑状态
     *
     * @param userId 用户ID（离开编辑的用户）
     */
    void leaveEditing(Long userId);

    /**
     * 刷新用户编辑状态（维持在线连接）
     *
     * @param pageId Wiki页面ID（用户当前编辑的页面）
     * @param userId 用户ID（需要刷新状态的用户）
     */
    void refreshEditingStatus(Long pageId, Long userId);

    /**
     * 校验用户是否正在编辑指定页面
     *
     * @param userId 用户ID
     * @param pageId Wiki页面ID
     * @return boolean  true-正在编辑，false-未编辑
     */
    boolean isUserEditing(Long userId, Long pageId);

    /**
     * 更新用户光标位置（用于多用户光标同步）
     *
     * @param userId   用户ID
     * @param position 光标位置信息（包含页面ID、行号、列号等）
     */
    void updateCursorPosition(Long userId, WikiCollaborationDTO.CursorPosition position);

    /**
     * 获取指定页面的在线编辑者列表
     *
     * @param pageId Wiki页面ID
     * @return List<WikiCollaborationDTO.EditorInfo> 在线编辑者信息列表（包含用户ID、用户名等）
     */
    List<WikiCollaborationDTO.EditorInfo> getOnlineEditors(Long pageId);

    /**
     * 获取指定页面所有编辑者的光标位置
     *
     * @param pageId Wiki页面ID
     * @return List<WikiCollaborationDTO.CursorPosition> 所有编辑者的光标位置列表
     */
    List<WikiCollaborationDTO.CursorPosition> getAllEditorsCursor(Long pageId);

    /**
     * 获取指定页面的在线编辑者数量
     *
     * @param pageId Wiki页面ID
     * @return Long 在线编辑者人数
     */
    Long getEditorCount(Long pageId);

    /**
     * 尝试锁定页面内容（避免并发编辑冲突）
     *
     * @param userId 用户ID（申请锁定的用户）
     * @param pageId Wiki页面ID（要锁定的页面）
     * @return boolean  true-锁定成功，false-已被其他用户锁定
     */
    boolean tryLockContent(Long userId, Long pageId);

    /**
     * 释放页面内容锁
     *
     * @param pageId Wiki页面ID（要释放锁定的页面）
     * @param userId 用户ID（释放锁定的用户，需与锁定用户一致）
     */
    void releaseLock(Long pageId, Long userId);
}