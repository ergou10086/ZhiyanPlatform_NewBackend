package hbnu.project.zhiyanbackend.auth.model.converter;

import hbnu.project.zhiyanbackend.auth.model.dto.AchievementLinkDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UpdateAchievementLinkDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.UserAchievement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 用户关联成果转换器
 *
 * @author ErgouTree
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserAchievementConverter {

    /**
     * 将AchievementLinkDTO转换为UserAchievement实体
     *
     * @param dto 关联学术成果请求DTO
     * @return UserAchievement实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "projectId", expression = "java( dto.getProjectId() != null ? Long.valueOf(dto.getProjectId()) : null )")
    @Mapping(target = "achievementId", expression = "java( Long.valueOf(dto.getAchievementId()) )")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserAchievement toEntity(AchievementLinkDTO dto);

    /**
     * 使用UpdateAchievementLinkDTO更新UserAchievement实体
     *
     * @param dto  更新成果关联信息请求DTO
     * @param entity 要更新的实体
     * @return 更新后的实体
     */
    UserAchievement updateEntity(UpdateAchievementLinkDTO dto, @MappingTarget UserAchievement entity);
}
