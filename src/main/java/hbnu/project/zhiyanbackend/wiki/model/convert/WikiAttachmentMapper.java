package hbnu.project.zhiyanbackend.wiki.model.convert;

import hbnu.project.zhiyanbackend.wiki.model.dto.WikiAttachmentDTO;
import hbnu.project.zhiyanbackend.wiki.model.entity.WikiAttachment;
import hbnu.project.zhiyanbackend.wiki.model.enums.AttachmentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface WikiAttachmentMapper {

    WikiAttachmentMapper INSTANCE = Mappers.getMapper(WikiAttachmentMapper.class);

    /**
     * 将WikiAttachment实体转换为WikiAttachmentDTO
     */
    @Mapping(target = "id", source = "id", qualifiedByName = "longToString")
    @Mapping(target = "wikiPageId", source = "wikiPageId", qualifiedByName = "longToString")
    @Mapping(target = "projectId", source = "projectId", qualifiedByName = "longToString")
    @Mapping(target = "attachmentType", source = "attachmentType", qualifiedByName = "attachmentTypeToString")
    @Mapping(target = "fileSizeFormatted", source = "fileSize", qualifiedByName = "formatFileSize")
    @Mapping(target = "uploadBy", source = "uploadBy", qualifiedByName = "longToStringNullable")
    @Mapping(target = "deleted", source = "isDeleted")
    @Mapping(target = "deletedBy", source = "deletedBy", qualifiedByName = "longToStringNullable")
    WikiAttachmentDTO toDTO(WikiAttachment attachment);

    /**
     * 将Long类型转换为String类型
     */
    @Named("longToString")
    default String longToString(Long value) {
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 处理可能为null的Long类型转换
     */
    @Named("longToStringNullable")
    default String longToStringNullable(Long value) {
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * 将AttachmentType枚举转换为字符串
     */
    @Named("attachmentTypeToString")
    default String attachmentTypeToString(AttachmentType type) {
        return type != null ? type.name() : null;
    }

    /**
     * 格式化文件大小（与原方法逻辑保持一致）
     */
    @Named("formatFileSize")
    default String formatFileSize(Long fileSize) {
        if (fileSize == null) {
            return null;
        }
        // 这里实现原有的文件大小格式化逻辑
        // 例如：
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }
}