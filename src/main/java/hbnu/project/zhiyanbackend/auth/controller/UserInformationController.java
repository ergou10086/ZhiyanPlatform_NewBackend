package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.UserInformationService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户个人信息控制器
 * 负责头像、个人信息、成果绑定相关接口
 * <p>
 * 请求路径说明：
 * - 头像相关：/zhiyan/auth/user-avatar/*
 * - 成果相关：/zhiyan/auth/users/achievements/*
 * - 个人资料：/zhiyan/auth/users/profile/*
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/auth")    // 未修改，保持
@RequiredArgsConstructor
@Tag(name = "用户个人信息", description = "头像、个人信息、成果绑定相关接口")
public class UserInformationController {

    private final UserInformationService userInformationService;

    private final UserRepository userRepository;

    // ==================== 头像管理接口 ====================

    /**
     * 上传用户头像
     * 前端处理裁剪
     * 路径: POST /zhiyan/auth/user-avatar/upload
     * 权限: 所有已登录用户（只能上传自己的头像）
     */
    @PostMapping("/user-avatar/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传头像", description = "上传用户头像")
    public R<AvatarDTO> uploadAvatar(
            @Parameter(description = "头像图片文件", required = true) @RequestParam("file") MultipartFile file
    ) {
        log.info("上传用户头像: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        try {
            // 获取用户ID
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                log.warn("上传头像失败: 用户未登录");
                return R.fail("用户未登录");
            }

            // 调用服务层上传头像
            R<AvatarDTO> result = userInformationService.uploadAvatar(userId, file);
            log.info("上传头像结果: code={}, msg={}, hasData={}", result.getCode(), result.getMsg(), result.getData() != null);
            return result;
        } catch (Exception e) {
            log.error("上传头像异常", e);
            return R.fail("上传头像异常: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户头像信息
     * 路径: GET /zhiyan/auth/user-avatar/me_avatar
     * 权限: 所有已登录用户
     */
    @GetMapping("/user-avatar/me_avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取头像信息", description = "获取当前用户的头像URL信息（包含所有尺寸）")
    public R<AvatarDTO> getMyAvatar() {
        log.info("获取当前用户头像信息");

        try {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            return userInformationService.getAvatarInfo(userId);
        } catch (Exception e) {
            log.error("获取头像信息失败", e);
            return R.fail("获取头像信息失败");
        }
    }

    /**
     * 根据用户ID获取头像信息
     * 路径: GET /zhiyan/auth/user-avatar/{userId}
     * 用于其他服务查询用户头像
     */
    @GetMapping("/user-avatar/{userId}")
    @Operation(summary = "获取指定用户头像", description = "根据用户ID获取头像信息（服务间调用）")
    public R<AvatarDTO> getAvatar(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId) {
        log.info("获取用户头像信息: userId={}", userId);

        try {
            return userInformationService.getAvatarInfo(userId);
        } catch (Exception e) {
            log.error("获取用户头像信息失败: userId={}", userId, e);
            return R.fail("获取头像信息失败");
        }
    }

    // ==================== 学术成果关联接口 ====================

    /**
     * 关联学术成果
     * 用户手动关联所在项目内的公开成果
     * 路径: POST /zhiyan/auth/users/achievements/link
     */
    @PostMapping("/users/achievements/link")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "关联学术成果", description = "用户手动关联所在项目内的公开成果")
    public R<UserAchievementDTO> linkAchievement(@Valid @RequestBody AchievementLinkDTO linkBody) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        log.info("用户[{}]关联学术成果: {}", userId, linkBody);
        return userInformationService.linkAchievement(userId, linkBody);
    }

    /**
     * 取消关联学术成果
     * 路径: DELETE /zhiyan/auth/users/achievements/{achievementId}
     */
    @DeleteMapping("/users/achievements/{achievementId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "取消关联学术成果", description = "取消用户与学术成果的关联")
    public R<Void> unlinkAchievement(
            @Parameter(description = "成果ID", required = true)
            @PathVariable Long achievementId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        log.info("用户[{}]取消关联成果[{}]", userId, achievementId);
        return userInformationService.unlinkAchievement(userId, achievementId);
    }

    /**
     * 更新成果关联信息（排序、备注）
     * 路径: PUT /zhiyan/auth/users/achievements/{achievementId}
     */
    @PutMapping("/users/achievements/{achievementId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新成果关联信息", description = "更新展示顺序或备注说明")
    public R<UserAchievementDTO> updateAchievementLink(
            @Parameter(description = "成果ID", required = true)
            @PathVariable Long achievementId,
            @Valid @RequestBody UpdateAchievementLinkDTO updateBody) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        log.info("用户[{}]更新成果[{}]关联信息", userId, achievementId);
        return userInformationService.updateAchievementLink(userId, achievementId, updateBody);
    }

    /**
     * 查询当前用户关联的所有成果
     * 路径: GET /zhiyan/auth/users/achievements/me
     */
    @GetMapping("/users/achievements/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "查询我的学术成果", description = "查询当前用户关联的所有学术成果")
    public R<List<UserAchievementDTO>> getMyAchievements() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("用户未登录");
        }

        log.info("查询用户[{}]的学术成果", userId);
        return userInformationService.getUserAllAchievements(userId);
    }

    /**
     * 查询指定用户的学术成果
     * 路径: GET /zhiyan/auth/users/achievements/user/{userId}
     */
    @GetMapping("/users/achievements/user/{userId}")
    @Operation(summary = "查询用户学术成果", description = "查询指定用户关联的学术成果（公开）")
    public R<List<UserAchievementDTO>> getUserAchievements(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        log.info("查询用户[{}]的学术成果", userId);
        return userInformationService.getUserAllAchievements(userId);
    }

    // ==================== 个人资料管理接口 ====================

    /**
     * 更新用户个人资料
     * 路径: PUT /zhiyan/auth/users/profile
     * 权限: 所有已登录用户（只能更新自己的资料）
     */
    @PutMapping("/users/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的个人信息")
    public R<UserDTO> updateProfile(
            @Valid @RequestBody UserUpdateDTO updateBody) {
        log.info("更新用户资料: {}", updateBody);

        try {
            // 获取当前用户ID
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                return R.fail("用户未登录");
            }

            // 调用服务层更新资料
            R<UserDTO> result = userInformationService.updateUserProfile(userId, updateBody);
            if (!R.isSuccess(result)) {
                return R.fail(result.getMsg());
            }

            return R.ok(result.getData(), "资料更新成功");
        } catch (Exception e) {
            log.error("更新用户资料失败", e);
            return R.fail("更新资料失败");
        }
    }


    /**
     * 更新用户研究方向标签
     */
    @PutMapping("/profile/research-tags")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新研究方向标签", description = "用户更新自己的研究方向标签（1-5个）")
    public R<List<String>> updateResearchTags(
            @Valid @RequestBody UpdateResearchTagsDTO body) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或令牌无效");
        }

        // 验证标签数量
        if (body.getResearchTags() == null || body.getResearchTags().isEmpty()) {
            return R.fail("研究方向标签不能为空");
        }

        if (body.getResearchTags().size() > 5) {
            return R.fail("研究方向标签最多5个");
        }

        // 验证标签和长度的接口
        for(String tag: body.getResearchTags()) {
            if (tag == null || tag.trim().isEmpty()) {
                return R.fail("标签内容不能为空");
            }
            if (tag.length() > 50) {
                return R.fail("单个标签长度不能超过50个字符");
            }
            // 防止XSS攻击
            if (tag.matches(".*[<>\"'].*")) {
                return R.fail("标签不能包含特殊字符");
            }
        }

        // 去重并限制数量
        List<String> uniqueTags = body.getResearchTags().stream()
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        // 更新用户标签
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ControllerException("用户不存在"));

        user.setResearchTagList(uniqueTags);
        userRepository.save(user);

        log.info("用户[{}]更新研究方向标签: {}", userId, uniqueTags);
        return R.ok(uniqueTags, "更新成功");
    }

    /**
     * 获取指定用户的研究方向标签
     * 路径: GET /zhiyan/auth/users/{userId}/research-tags
     * 用于他人主页查看研究方向（公开信息）
     */
    @GetMapping("/users/{userId}/research-tags")
    @Operation(summary = "查询用户研究方向标签", description = "根据用户ID查询其研究方向标签列表")
    public R<List<String>> getUserResearchTags(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        return userInformationService.getUserResearchTags(userId);
    }
}
