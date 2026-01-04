package hbnu.project.zhiyanbackend.auth.model.converter;

import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserInfoResponseDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户实体转换器
 * 使用 MapStruct 进行实体与 DTO 之间的转换
 *
 * @author ErgouTree
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * 实体转DTO（不包含头像数据，避免传输大量二进制数据）
     */
    @Named("userToDTO")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "researchTags", expression = "java(user.getResearchTagList())")
    @Mapping(target = "profileLinks", expression = "java(user.getProfileLinkList())")
    UserDTO toDTO(User user);

    /**
     * 实体转DTO（包含头像 URL）
     */
    // TODO COS_AVATAR_MIGRATE: 使用 User.avatarUrl（COS URL）映射到 DTO
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "researchTags", expression = "java(user.getResearchTagList())")
    @Mapping(target = "profileLinks", expression = "java(user.getProfileLinkList())")
    UserDTO toDTOWithAvatar(User user);

    /**
     * DTO转实体
     */
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "researchTags", ignore = true)
    @Mapping(target = "profileLinks", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    User toEntity(UserDTO dto);

    /**
     * 将 UserDTO 转换为 UserInfoResponseDTO（包含角色和权限）
     */
    @Mapping(target = "avatarUrl", source = "avatarUrl") // 直接从 DTO 中的头像 URL 映射
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatus")
    @Mapping(target = "createdAt", ignore = true) // 这些字段在 UserDTO 中不存在
    @Mapping(target = "updatedAt", ignore = true)
    UserInfoResponseDTO toUserInfoResponseDTOwithRoles(UserDTO userDTO);

    /**
     * 将 UserDTO 转换为 UserInfoResponseDTO（不包含角色权限）
     */
    // TODO COS_AVATAR_MIGRATE: 从 DTO 中的 avatarUrl 字段映射，停止把 Base64 avatarData 当作 URL
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatus")
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "twoFactorEnabled", source = "twoFactorEnabled")
    UserInfoResponseDTO toUserInfoResponse(UserDTO userDTO);

    /**
     * 将 UserStatus 枚举转换为字符串
     */
    @Named("mapStatus")
    default String mapStatus(hbnu.project.zhiyanbackend.auth.model.enums.UserStatus status) {
        return status != null ? status.name() : null;
    }

    /**
     * 实体列表转DTO列表
     */
    @IterableMapping(qualifiedByName = "userToDTO")
    List<UserDTO> toDTOList(List<User> users);
}

