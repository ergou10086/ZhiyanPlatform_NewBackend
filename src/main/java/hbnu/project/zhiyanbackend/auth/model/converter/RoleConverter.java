package hbnu.project.zhiyanbackend.auth.model.converter;

import hbnu.project.zhiyanbackend.auth.model.dto.RoleDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 角色实体转换器
 *
 * @author ErgouTree
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);

    /**
     * 实体转DTO
     */
    @Mapping(target = "permissions", ignore = true)
    RoleDTO toDTO(Role role);

    /**
     * DTO转实体
     */
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "rolePermissions", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Role toEntity(RoleDTO dto);

    /**
     * 实体列表转DTO列表
     */
    List<RoleDTO> toDTOList(List<Role> roles);
}

