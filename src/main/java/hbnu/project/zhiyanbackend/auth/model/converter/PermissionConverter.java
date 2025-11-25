package hbnu.project.zhiyanbackend.auth.model.converter;

import hbnu.project.zhiyanbackend.auth.model.dto.PermissionDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 权限实体转换器
 *
 * @author ErgouTree
 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    PermissionConverter INSTANCE = Mappers.getMapper(PermissionConverter.class);

    /**
     * 实体转DTO
     */
    PermissionDTO toDTO(Permission permission);

    /**
     * DTO转实体
     */
    @Mapping(target = "rolePermissions", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Permission toEntity(PermissionDTO dto);

    /**
     * 实体列表转DTO列表
     */
    List<PermissionDTO> toDTOList(List<Permission> permissions);
}

