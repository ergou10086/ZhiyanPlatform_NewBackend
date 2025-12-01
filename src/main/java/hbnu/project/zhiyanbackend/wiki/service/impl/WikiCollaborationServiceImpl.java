package hbnu.project.zhiyanbackend.wiki.service.impl;

import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.service.UserService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.wiki.model.dto.WikiCollaborationDTO;
import hbnu.project.zhiyanbackend.wiki.service.WikiCollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Wiki 协同编辑服务实现
 *
 * <p>使用 Redis 管理在线编辑者、光标位置和简单内容锁，保持与旧架构功能一致。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCollaborationServiceImpl implements WikiCollaborationService {

    private static final String PAGE_EDITORS_KEY = "wiki:editors:page:";
    private static final String USER_CURSOR_KEY = "wiki:cursor:user:";
    private static final String USER_PAGE_KEY = "wiki:user:page:";
    private static final String PAGE_CONTENT_LOCK_KEY = "wiki:lock:page:";

    private static final long EDITOR_TTL_SECONDS = 300L;
    private static final long CURSOR_TTL_SECONDS = 60L;
    private static final long CONTENT_LOCK_TTL_SECONDS = 30L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;

    /**
     * 用户加入Wiki页面编辑
     *
     * @param pageId Wiki页面ID（用户要编辑的页面）
     * @param userId 用户ID（加入编辑的用户）
     */
    @Override
    public void joinEditing(Long pageId, Long userId) {
        try {
            String pageKey = PAGE_EDITORS_KEY + pageId;
            redisTemplate.opsForSet().add(pageKey, userId.toString());
            redisTemplate.expire(pageKey, Duration.ofSeconds(EDITOR_TTL_SECONDS));

            String userPageKey = USER_PAGE_KEY + userId;
            redisTemplate.opsForValue().set(userPageKey, pageId.toString(), EDITOR_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("用户 [{}] 加入页面 [{}] 编辑", userId, pageId);
        } catch (Exception e) {
            log.error("用户加入编辑失败: pageId={}, userId={}", pageId, userId, e);
            throw new ServiceException("加入协同编辑失败");
        }
    }

    /**
     * 用户离开编辑状态
     *
     * @param userId 用户ID（离开编辑的用户）
     */
    @Override
    public void leaveEditing(Long userId) {
        try {
            String userPageKey = USER_PAGE_KEY + userId;
            Object pageIdObj = redisTemplate.opsForValue().get(userPageKey);

            if (pageIdObj != null) {
                Long pageId = Long.parseLong(pageIdObj.toString());
                String pageKey = PAGE_EDITORS_KEY + pageId;
                redisTemplate.opsForSet().remove(pageKey, userId.toString());
                Long size = redisTemplate.opsForSet().size(pageKey);
                if (size != null && size == 0) {
                    redisTemplate.delete(pageKey);
                }
                log.debug("用户 [{}] 离开页面 [{}] 编辑", userId, pageId);
            }

            redisTemplate.delete(userPageKey);
            redisTemplate.delete(USER_CURSOR_KEY + userId);
        } catch (Exception e) {
            log.error("用户离开编辑失败: userId={}", userId, e);
            throw new ServiceException("退出协同编辑失败");
        }
    }

    /**
     * 刷新用户编辑状态（维持在线连接）
     *
     * @param pageId Wiki页面ID（用户当前编辑的页面）
     * @param userId 用户ID（需要刷新状态的用户）
     */
    @Override
    public void refreshEditingStatus(Long pageId, Long userId) {
        try {
            redisTemplate.expire(PAGE_EDITORS_KEY + pageId, Duration.ofSeconds(EDITOR_TTL_SECONDS));
            redisTemplate.expire(USER_PAGE_KEY + userId, Duration.ofSeconds(EDITOR_TTL_SECONDS));
        } catch (Exception e) {
            log.error("刷新协同编辑状态失败: pageId={}, userId={}", pageId, userId, e);
        }
    }

    /**
     * 校验用户是否正在编辑指定页面
     *
     * @param userId 用户ID
     * @param pageId Wiki页面ID
     * @return boolean  true-正在编辑，false-未编辑
     */
    @Override
    public boolean isUserEditing(Long userId, Long pageId) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(PAGE_EDITORS_KEY + pageId, userId.toString());
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("检查用户编辑状态失败: userId={}, pageId={}", userId, pageId, e);
            return false;
        }
    }

    /**
     * 更新用户光标位置（用于多用户光标同步）
     *
     * @param userId   用户ID
     * @param position 光标位置信息（包含页面ID、行号、列号等）
     */
    @Override
    public void updateCursorPosition(Long userId, WikiCollaborationDTO.CursorPosition position) {
        try {
            position.setLastUpdate(LocalDateTime.now());
            redisTemplate.opsForValue().set(
                    USER_CURSOR_KEY + userId,
                    position,
                    CURSOR_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("更新光标位置失败: userId={}", userId, e);
            throw new ServiceException("更新光标位置失败");
        }
    }

    /**
     * 获取指定页面的在线编辑者列表
     *
     * @param pageId Wiki页面ID
     * @return List<WikiCollaborationDTO.EditorInfo> 在线编辑者信息列表（包含用户ID、用户名等）
     */
    @Override
    public List<WikiCollaborationDTO.EditorInfo> getOnlineEditors(Long pageId) {
        try {
            Set<Object> userIdsObj = redisTemplate.opsForSet().members(PAGE_EDITORS_KEY + pageId);
            if (userIdsObj == null || userIdsObj.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> userIds = userIdsObj.stream()
                    .filter(Objects::nonNull)
                    .map(obj -> Long.parseLong(obj.toString()))
                    .collect(Collectors.toList());

            R<List<UserDTO>> userResult = userService.getUsersByIds(userIds);
            if (userResult == null || !R.isSuccess(userResult) || userResult.getData() == null) {
                log.warn("获取用户信息失败: userIds={}", userIds);
                return Collections.emptyList();
            }

            return userResult.getData().stream()
                    .map(user -> WikiCollaborationDTO.EditorInfo.builder()
                            .userId(user.getId())
                            .username(user.getName())
                            .avatar(user.getAvatarData())
                            .joinTime(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取在线编辑者失败: pageId={}", pageId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定页面所有编辑者的光标位置
     *
     * @param pageId Wiki页面ID
     * @return List<WikiCollaborationDTO.CursorPosition> 所有编辑者的光标位置列表
     */
    @Override
    public List<WikiCollaborationDTO.CursorPosition> getAllEditorsCursor(Long pageId) {
        try {
            Set<Object> userIdsObj = redisTemplate.opsForSet().members(PAGE_EDITORS_KEY + pageId);
            if (userIdsObj == null || userIdsObj.isEmpty()) {
                return Collections.emptyList();
            }

            List<WikiCollaborationDTO.CursorPosition> positions = new ArrayList<>();
            for (Object userIdObj : userIdsObj) {
                if (userIdObj == null) {
                    continue;
                }
                long userId = Long.parseLong(userIdObj.toString());
                Object value = redisTemplate.opsForValue().get(USER_CURSOR_KEY + userId);
                if (value instanceof WikiCollaborationDTO.CursorPosition position) {
                    if (position.getLastUpdate() == null ||
                            position.getLastUpdate().isAfter(LocalDateTime.now().minusMinutes(5))) {
                        positions.add(position);
                    }
                }
            }
            return positions;
        } catch (Exception e) {
            log.error("获取光标位置失败: pageId={}", pageId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定页面的在线编辑者数量
     *
     * @param pageId Wiki页面ID
     * @return Long 在线编辑者人数
     */
    @Override
    public Long getEditorCount(Long pageId) {
        try {
            Long size = redisTemplate.opsForSet().size(PAGE_EDITORS_KEY + pageId);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("获取编辑者数量失败: pageId={}", pageId, e);
            return 0L;
        }
    }

    /**
     * 尝试锁定页面内容（避免并发编辑冲突）
     *
     * @param userId 用户ID（申请锁定的用户）
     * @param pageId Wiki页面ID（要锁定的页面）
     * @return boolean  true-锁定成功，false-已被其他用户锁定
     */
    @Override
    public boolean tryLockContent(Long userId, Long pageId) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    PAGE_CONTENT_LOCK_KEY + pageId,
                    userId.toString(),
                    CONTENT_LOCK_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("获取内容锁失败: pageId={}, userId={}", pageId, userId, e);
            return false;
        }
    }

    /**
     * 释放页面内容锁
     *
     * @param pageId Wiki页面ID（要释放锁定的页面）
     * @param userId 用户ID（释放锁定的用户，需与锁定用户一致）
     */
    @Override
    public void releaseLock(Long pageId, Long userId) {
        try {
            String lockKey = PAGE_CONTENT_LOCK_KEY + pageId;
            Object owner = redisTemplate.opsForValue().get(lockKey);
            if (owner != null && owner.toString().equals(userId.toString())) {
                redisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.error("释放内容锁失败: pageId={}, userId={}", pageId, userId, e);
        }
    }
}

