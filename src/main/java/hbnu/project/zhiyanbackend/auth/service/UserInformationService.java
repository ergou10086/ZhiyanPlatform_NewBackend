package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.*;
import hbnu.project.zhiyanbackend.basic.domain.R;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户个人信息服务
 * 主要处理用户个人信息的部分修改，对应前端个人信息
 * 但修改用户关键信息不在这里
 *
 * @author ErgouTree
 */
public interface UserInformationService {

    /**
     * 更新用户个人资料
     * 普通用户只能更新自己的资料
     *
     * @param userId 用户ID
     * @param updateDTO 更新内容
     * @return 更新后的用户信息
     */
    R<UserDTO> updateUserProfile(Long userId, UserUpdateDTO updateDTO);

    /**
     * 上传用户头像
     * 前端已完成裁剪，后端生成多尺寸缩略图
     *
     * @param userId 用户ID
     * @param file   图片文件（已裁剪）
     * @return 头像URL信息
     */
    R<AvatarDTO> uploadAvatar(Long userId, MultipartFile file);

    /**
     * 获取用户头像信息
     * 根据用户ID获取头像的Base64编码数据
     *
     * @param userId 用户ID
     * @return 头像信息
     */
    R<AvatarDTO> getAvatarInfo(Long userId);

    /**
     * 关联学术成果
     *
     * @param userId 用户ID
     * @param linkBody 关联请求
     * @return 关联结果
     */
    R<UserAchievementDTO> linkAchievement(Long userId, AchievementLinkDTO linkBody);

    /**
     * 取消关联学术成果
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     * @return 操作结果
     */
    R<Void> unlinkAchievement(Long userId, Long achievementId);


    /**
     * 更新成果关联信息（排序、备注）
     *
     * @param userId 用户ID
     * @param achievementId 成果ID
     * @param updateBody 更新内容
     * @return 更新结果
     */
    R<UserAchievementDTO> updateAchievementLink(Long userId, Long achievementId, UpdateAchievementLinkDTO updateBody);

    /**
     * 查询用户关联的所有成果
     *
     * @param userId 用户ID
     * @return 成果列表
     */
    R<List<UserAchievementDTO>> getUserAllAchievements(Long userId);
}
