package hbnu.project.zhiyanbackend.auth.service.impl;

import hbnu.project.zhiyanbackend.auth.model.dto.AvatarDTO;
import hbnu.project.zhiyanbackend.auth.model.converter.UserConverter;
import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.model.entity.UserAchievement;
import hbnu.project.zhiyanbackend.auth.repository.UserAchievementRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.UserInformationService;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.basic.domain.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * 用户个人信息服务实现类
 * 提供用户个人资料管理、头像上传、学术成果关联等功能
 * 
 * 实现说明：
 * 1. 用户个人资料更新（姓名、职称、机构等）
 * 2. 头像上传（存储到PostgreSQL BYTEA字段）
 * 3. 学术成果关联管理
 * 4. 事务保证数据一致性
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInformationServiceImpl implements UserInformationService {

    private final UserRepository userRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserConverter userConverter;

    // 头像相关常量，限制5MB
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");

    /**
     * 更新用户个人资料
     * 普通用户只能更新自己的资料
     * 注意：头像更新通过uploadAvatar方法单独处理
     *
     * @param userId 用户ID
     * @param updateDTO 更新内容
     * @return 更新后的用户信息
     */
    @Override
    @Transactional
    public R<UserDTO> updateUserProfile(Long userId, UserUpdateDTO updateDTO) {
        try {
            log.info("更新用户个人资料 - userId: {}", userId);

            // 1. 验证用户是否存在
            Optional<User> optionalUser = userRepository.findByIdAndIsDeletedFalse(userId);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在 - userId: {}", userId);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();

            // 2. 更新用户信息（只更新非空字段）
            if (StringUtils.isNotBlank(updateDTO.getName())) {
                user.setName(updateDTO.getName());
            }
            if (StringUtils.isNotBlank(updateDTO.getTitle())) {
                user.setTitle(updateDTO.getTitle());
            }
            if (StringUtils.isNotBlank(updateDTO.getInstitution())) {
                user.setInstitution(updateDTO.getInstitution());
            }

            // 3. 保存更新
            User savedUser = userRepository.save(user);

            // 4. 转换为DTO返回
            UserDTO userDTO = userConverter.toDTO(savedUser);

            log.info("用户个人资料更新成功 - userId: {}", userId);
            return R.ok(userDTO, "个人资料更新成功");
        } catch (Exception e) {
            log.error("更新用户个人资料失败 - userId: {}", userId, e);
            return R.fail("更新个人资料失败: " + e.getMessage());
        }
    }

    /**
     * 上传用户头像
     * 前端已完成裁剪，后端直接存储到PostgreSQL BYTEA字段
     *
     * @param userId 用户ID
     * @param file 图片文件（已裁剪）
     * @return 头像URL信息
     */
    @Override
    @Transactional
    public R<AvatarDTO> uploadAvatar(Long userId, MultipartFile file) {
        try {
            log.info("开始上传用户头像: userId={}, filename={}", userId, file.getOriginalFilename());

            // 1. 验证用户是否存在
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(userId);
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }
            User user = userOpt.get();

            // 2. 验证文件
            validateAvatarFile(file);

            // 3. 读取文件内容
            byte[] avatarBytes = file.getBytes();
            String contentType = file.getContentType();

            // 4. 更新用户头像信息并存储
            user.setAvatarData(avatarBytes);
            user.setAvatarContentType(contentType);
            user.setAvatarSize(file.getSize());

            userRepository.save(user);

            // 5. 构建返回DTO
            AvatarDTO avatarDTO = AvatarDTO.builder()
                    .avatarData(Base64.getEncoder().encodeToString(avatarBytes))
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();

            log.info("用户头像上传成功: userId={}, size={} bytes", userId, file.getSize());
            return R.ok(avatarDTO, "头像上传成功");
        } catch (IOException e) {
            log.error("读取头像文件失败: userId={}", userId, e);
            return R.fail("读取头像文件失败");
        } catch (IllegalArgumentException e) {
            log.error("头像文件验证失败: userId={}, error={}", userId, e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("头像上传异常: userId={}", userId, e);
            return R.fail("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 关联学术成果
     * 用户可以将公开的学术成果关联到自己的个人资料
     *
     * @param userId 用户ID
     * @param linkBody 关联请求
     * @return 关联结果
     */
    @Override
    @Transactional
    public R<UserAchievementDTO> linkAchievement(Long userId, AchievementLinkDTO linkBody) {
        try {
            log.info("用户[{}]关联成果: achievementId={}, projectId={}",
                    userId, linkBody.getAchievementId(), linkBody.getProjectId());

            // 1. 参数验证
            if (StringUtils.isBlank(linkBody.getAchievementId())) {
                return R.fail("成果ID不能为空");
            }

            // 2. 转换ID类型（DTO中是String，实体中是Long）
            Long achievementId;
            Long projectId = null;
            try {
                achievementId = Long.parseLong(linkBody.getAchievementId());
                if (StringUtils.isNotBlank(linkBody.getProjectId())) {
                    projectId = Long.parseLong(linkBody.getProjectId());
                }
            } catch (NumberFormatException e) {
                return R.fail("成果ID或项目ID格式错误");
            }

            // 3. 检查是否已经被关联
            if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievementId)) {
                return R.fail("该成果已关联，请勿重复操作");
            }

            // 4. 限制用户最多关联10个成果
            long count = userAchievementRepository.countByUserId(userId);
            if (count >= 10) {
                return R.fail("最多只能关联10个学术成果");
            }

            // 5. TODO: 调用知识库服务验证成果是否存在且公开
            // 原架构中通过KnowledgeServiceClient验证，新架构需要后续集成
            // R<Object> achievementResult = knowledgeServiceClient.getAchievementById(achievementId);
            // if (!R.isSuccess(achievementResult)) {
            //     return R.fail("成果不存在或无权访问");
            // }

            // 6. 创建关联记录
            UserAchievement userAchievement = UserAchievement.builder()
                    .userId(userId)
                    .achievementId(achievementId)
                    // 如果未提供项目ID，使用0作为默认值
                    .projectId(projectId != null ? projectId : 0L)
                    .displayOrder(linkBody.getDisplayOrder() != null ? linkBody.getDisplayOrder() : 0)
                    .remark(linkBody.getRemark())
                    .build();

            userAchievementRepository.save(userAchievement);

            // 7. 构建返回DTO
            UserAchievementDTO dto = buildUserAchievementDTO(userAchievement, null);

            log.info("用户[{}]成功关联成果[{}]", userId, achievementId);
            return R.ok(dto, "关联成功");
        } catch (Exception e) {
            log.error("关联学术成果失败 - userId: {}, achievementId: {}", userId, linkBody.getAchievementId(), e);
            return R.fail("关联成果失败: " + e.getMessage());
        }
    }

    /**
     * 取消关联学术成果
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> unlinkAchievement(Long userId, Long achievementId) {
        try {
            log.info("用户[{}]取消关联成果: achievementId={}", userId, achievementId);

            // 1. 查找关联记录
            Optional<UserAchievement> optionalUserAchievement = userAchievementRepository
                    .findByUserIdAndAchievementId(userId, achievementId);

            if (optionalUserAchievement.isEmpty()) {
                return R.fail("未找到关联记录");
            }

            // 2. 删除关联记录
            userAchievementRepository.delete(optionalUserAchievement.get());

            log.info("用户[{}]成功取消关联成果[{}]", userId, achievementId);
            return R.ok(null, "取消关联成功");
        } catch (Exception e) {
            log.error("取消关联学术成果失败 - userId: {}, achievementId: {}", userId, achievementId, e);
            return R.fail("取消关联失败: " + e.getMessage());
        }
    }

    /**
     * 更新成果关联信息（排序、备注）
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     * @param updateBody 更新内容
     * @return 更新结果
     */
    @Override
    @Transactional
    public R<UserAchievementDTO> updateAchievementLink(Long userId, Long achievementId, UpdateAchievementLinkDTO updateBody) {
        try {
            log.info("用户[{}]更新成果关联信息: achievementId={}", userId, achievementId);

            // 1. 查找关联记录
            Optional<UserAchievement> optionalUserAchievement = userAchievementRepository
                    .findByUserIdAndAchievementId(userId, achievementId);

            if (optionalUserAchievement.isEmpty()) {
                return R.fail("未找到关联记录");
            }

            UserAchievement userAchievement = optionalUserAchievement.get();

            // 2. 更新字段
            if (updateBody.getDisplayOrder() != null) {
                userAchievement.setDisplayOrder(updateBody.getDisplayOrder());
            }
            if (updateBody.getRemark() != null) {
                userAchievement.setRemark(updateBody.getRemark());
            }

            // 3. 保存更新
            userAchievementRepository.save(userAchievement);

            // 4. 构建返回DTO
            UserAchievementDTO dto = buildUserAchievementDTO(userAchievement, null);

            log.info("用户[{}]成功更新成果关联信息[{}]", userId, achievementId);
            return R.ok(dto, "更新成功");
        } catch (Exception e) {
            log.error("更新成果关联信息失败 - userId: {}, achievementId: {}", userId, achievementId, e);
            return R.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户头像信息
     * 根据用户ID获取头像的Base64编码数据
     *
     * @param userId 用户ID
     * @return 头像信息
     */
    @Override
    @Transactional(readOnly = true)
    public R<AvatarDTO> getAvatarInfo(Long userId) {
        try {
            log.info("获取用户头像信息: userId={}", userId);

            // 1. 验证用户是否存在
            Optional<User> optionalUser = userRepository.findByIdAndIsDeletedFalse(userId);
            if (optionalUser.isEmpty()) {
                log.warn("用户不存在 - userId: {}", userId);
                return R.fail("用户不存在");
            }

            User user = optionalUser.get();

            // 2. 检查是否有头像
            if (user.getAvatarData() == null || user.getAvatarData().length == 0) {
                log.info("用户[{}]没有头像", userId);
                return R.ok(AvatarDTO.builder().build(), "用户暂无头像");
            }

            // 3. 构建返回DTO
            AvatarDTO avatarDTO = AvatarDTO.builder()
                    .avatarData(Base64.getEncoder().encodeToString(user.getAvatarData()))
                    .contentType(user.getAvatarContentType())
                    .size(user.getAvatarSize())
                    .build();

            log.info("获取用户头像信息成功: userId={}, size={} bytes", userId, user.getAvatarSize());
            return R.ok(avatarDTO);
        } catch (Exception e) {
            log.error("获取用户头像信息失败 - userId: {}", userId, e);
            return R.fail("获取头像信息失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户关联的所有成果
     * 按展示顺序排序返回
     *
     * @param userId 用户ID
     * @return 成果列表
     */
    @Override
    @Transactional(readOnly = true)
    public R<List<UserAchievementDTO>> getUserAllAchievements(Long userId) {
        try {
            log.info("查询用户[{}]的所有关联成果", userId);

            // 1. 查询用户关联的所有成果
            List<UserAchievement> userAchievements = userAchievementRepository
                    .findByUserIdOrderByDisplayOrderAsc(userId);

            if (userAchievements.isEmpty()) {
                return R.ok(new ArrayList<>());
            }

            // 2. TODO: 批量查询成果信息（需要调用知识库服务）
            // 原架构中通过KnowledgeServiceClient批量查询，新架构需要后续集成
            // String achievementIds = userAchievements.stream()
            //         .map(ua -> ua.getAchievementId().toString())
            //         .collect(Collectors.joining(","));
            // R<Object> batchResult = knowledgeServiceClient.getAchievementsByIds(achievementIds);
            // Map<Long, Map<String, Object>> achievementMap = ...;

            // 3. 构建DTO列表
            List<UserAchievementDTO> dtoList = userAchievements.stream()
                    .map(ua -> buildUserAchievementDTO(ua, null))
                    .collect(Collectors.toList());

            log.info("查询用户[{}]关联成果成功，共{}个", userId, dtoList.size());
            return R.ok(dtoList);
        } catch (Exception e) {
            log.error("查询用户关联成果失败 - userId: {}", userId, e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public R<List<String>> getUserResearchTags(Long userId) {
        try {
            log.info("查询用户[{}]的研究方向标签", userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ControllerException("用户不存在"));

            List<String> tags = user.getResearchTagList();
            if (tags == null) {
                tags = new ArrayList<>();
            }

            return R.ok(tags);
        } catch (Exception e) {
            log.error("查询用户研究方向标签失败 - userId: {}", userId, e);
            return R.fail("查询失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证头像文件
     *
     * @param file 文件对象
     * @throws IllegalArgumentException 验证失败时抛出
     */
    private void validateAvatarFile(MultipartFile file) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件大小不能超过5MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持: " + String.join(", ", ALLOWED_IMAGE_TYPES));
        }
    }

    /**
     * 构建用户成果关联DTO
     *
     * @param userAchievement 用户成果关联实体
     * @param achievementData 成果详细信息（可选，从知识库服务获取）
     * @return 用户成果关联DTO
     */
    private UserAchievementDTO buildUserAchievementDTO(UserAchievement userAchievement, Map<String, Object> achievementData) {
        UserAchievementDTO dto = UserAchievementDTO.builder()
                .id(userAchievement.getId() != null ? userAchievement.getId().toString() : null)
                .userId(userAchievement.getUserId() != null ? userAchievement.getUserId().toString() : null)
                .achievementId(userAchievement.getAchievementId() != null ? userAchievement.getAchievementId().toString() : null)
                .projectId(userAchievement.getProjectId() != null ? userAchievement.getProjectId().toString() : null)
                .displayOrder(userAchievement.getDisplayOrder())
                .remark(userAchievement.getRemark())
                .createdAt(userAchievement.getCreatedAt())
                .build();

        // 填充成果信息（如果提供了成果数据）
        if (achievementData != null) {
            dto.setAchievementTitle((String) achievementData.get("title"));
            dto.setAchievementType((String) achievementData.get("type"));
            dto.setAchievementStatus((String) achievementData.get("status"));
        }

        return dto;
    }
}
