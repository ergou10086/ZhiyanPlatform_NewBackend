package hbnu.project.zhiyanbackend.knowledge.model.converter;

import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementFileDTO;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 成果文件相关实体转换器
 *
 * @author ErgouTree
 */
@Mapper(componentModel = "spring")
public interface AchievementFileConverter{
    AchievementFileConverter INSTANCE = Mappers.getMapper(AchievementFileConverter.class);

    /**
     * 实体转DTO
     * minioUrl 字段映射到 fileUrl
     */
    @Named("fileToDTO")
    @Mapping(target = "fileUrl", source = "cosUrl")
    AchievementFileDTO toDTO(AchievementFile file);

    /**
     * DTO转实体
     */
    @Mapping(target = "cosUrl", source = "fileUrl")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "achievement", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AchievementFile toEntity(AchievementFileDTO dto);

    /**
     * 实体列表转DTO列表
     */
    @IterableMapping(qualifiedByName = "fileToDTO")
    List<AchievementFileDTO> toDTOList(List<AchievementFile> files);
}

