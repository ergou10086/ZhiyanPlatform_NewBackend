package hbnu.project.zhiyanbackend.knowledge.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.knowledge.model.dto.*;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementDetail;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementType;

import hbnu.project.zhiyanbackend.tasks.model.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 成果相关实体转换器
 * 使用 MapStruct 进行对象转换
 *
 * @author ErgouTree
 */
@Mapper(componentModel = "spring")
public interface AchievementConverter {
    
    AchievementConverter INSTANCE = Mappers.getMapper(AchievementConverter.class);

    // ==================== Achievement 相关转换 ====================

    /**
     * Achievement 转 AchievementDTO（列表展示用）
     */
    @Mapping(target = "id", expression = "java(String.valueOf(achievement.getId()))")
    @Mapping(target = "projectId", expression = "java(String.valueOf(achievement.getProjectId()))")
    @Mapping(target = "creatorId", expression = "java(String.valueOf(achievement.getCreatorId()))")
    @Mapping(target = "typeName", expression = "java(getTypeName(achievement.getType()))")
    @Mapping(target = "fileCount", expression = "java(getFileCount(achievement))")
    @Mapping(target = "abstractText", expression = "java(getAbstractText(achievement))")
    @Mapping(target = "creatorName", ignore = true) // 需要从auth服务获取
    AchievementDTO toDTO(Achievement achievement);

    /**
     * Achievement 列表转 AchievementDTO 列表
     */
    List<AchievementDTO> toDTOList(List<Achievement> achievements);

    /**
     * Achievement 转 AchievementDetailDTO（详情展示用）
     */
    @Mapping(target = "id", expression = "java(String.valueOf(achievement.getId()))")
    @Mapping(target = "projectId", expression = "java(String.valueOf(achievement.getProjectId()))")
    @Mapping(target = "creatorId", expression = "java(String.valueOf(achievement.getCreatorId()))")
    @Mapping(target = "typeName", expression = "java(getTypeName(achievement.getType()))")
    @Mapping(target = "fileCount", expression = "java(getFileCount(achievement))")
    @Mapping(target = "abstractText", expression = "java(getAbstractFromDetail(achievement))")
    @Mapping(target = "detailData", expression = "java(parseDetailData(achievement.getDetail()))")
    @Mapping(target = "files", expression = "java(convertFiles(achievement.getFiles()))")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    @Mapping(target = "projectName", ignore = true) // 需要从其他服务获取
    @Mapping(target = "creatorName", ignore = true) // 需要从其他服务获取
    @Mapping(target = "tags", ignore = true) // 暂不支持标签
    AchievementDetailDTO toDetailDTO(Achievement achievement);

    /**
     * CreateAchievementDTO 转 Achievement（部分字段，用于创建）
     * 注意：id、detail、files 需要单独处理
     */
    @Mapping(target = "id", ignore = true) // 由雪花算法生成
    @Mapping(target = "status", expression = "java(getStatusOrDefault(dto.getStatus()))")
    @Mapping(target = "isPublic", expression = "java(getIsPublicOrDefault(dto.getIsPublic()))")
    @Mapping(target = "detail", ignore = true) // 需要单独处理
    @Mapping(target = "files", ignore = true) // 需要单独处理
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Achievement toEntity(CreateAchievementDTO dto);

    @Mapping(target = "status", expression = "java(task.getStatus() == null ? null : task.getStatus().name())")
    @Mapping(target = "priority", expression = "java(task.getPriority() == null ? null : task.getPriority().name())")
    TaskResultTaskRefDTO toTaskResultTaskRefDTO(Task task);

    // ==================== 辅助方法 ====================

    /**
     * 获取类型中文名称
     */
    @Named("getTypeName")
    default String getTypeName(AchievementType type) {
        if (type == null) {
            return "";
        }
        return type.getName();
    }

    /**
     * 获取文件数量
     */
    @Named("getFileCount")
    default Integer getFileCount(Achievement achievement) {
        if (achievement == null || achievement.getFiles() == null) {
            return 0;
        }
        return achievement.getFiles().size();
    }

    /**
     * 从 Achievement 的 Detail 获取摘要（用于列表展示，截取前200字符）
     */
    @Named("getAbstractText")
    default String getAbstractText(Achievement achievement) {
        if (achievement == null || achievement.getDetail() == null) {
            return null;
        }
        String abstractText = achievement.getDetail().getAbstractText();
        if (abstractText != null && abstractText.length() > 200) {
            return abstractText.substring(0, 200) + "...";
        }
        return abstractText;
    }

    /**
     * 从 Achievement 的 Detail 获取完整摘要
     */
    @Named("getAbstractFromDetail")
    default String getAbstractFromDetail(Achievement achievement) {
        if (achievement == null || achievement.getDetail() == null) {
            return null;
        }
        return achievement.getDetail().getAbstractText();
    }

    /**
     * 解析详情数据JSON为Map
     */
    @Named("parseDetailData")
    default Map<String, Object> parseDetailData(AchievementDetail detail) {
        if (detail == null || detail.getDetailData() == null) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> dataMap = JsonUtils.parseObject(
                    detail.getDetailData(),
                    new TypeReference<>() {
                    }
            );
            return dataMap != null ? dataMap : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 转换文件列表
     */
    @Named("convertFiles")
    default List<AchievementFileDTO> convertFiles(List<AchievementFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用 AchievementFileConverter 进行转换
        return AchievementFileConverter.INSTANCE.toDTOList(files);
    }

    /**
     * 获取状态或默认值
     */
    @Named("getStatusOrDefault")
    default AchievementStatus getStatusOrDefault(AchievementStatus status) {
        return status != null ? status : AchievementStatus.draft;
    }

    /**
     * 获取公开性或默认值（默认为私有）
     */
    @Named("getIsPublicOrDefault")
    default Boolean getIsPublicOrDefault(Boolean isPublic) {
        return isPublic != null ? isPublic : false;
    }
}
