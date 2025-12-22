// UserConnectionMapper.java
package hbnu.project.zhiyanbackend.auth.model.converter;

import hbnu.project.zhiyanbackend.auth.model.entity.UserConnection;
import hbnu.project.zhiyanbackend.auth.model.dto.UserConnectionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserConnectionMapper {
    
    UserConnectionMapper INSTANCE = Mappers.getMapper(UserConnectionMapper.class);
    
    /**
     * 将 UserConnection 实体转换为 UserConnectionDTO
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "provider", target = "provider")
    @Mapping(source = "providerUserId", target = "providerUserId")
    @Mapping(source = "providerUsername", target = "providerUsername")
    @Mapping(source = "providerEmail", target = "providerEmail")
    @Mapping(source = "providerAvatarUrl", target = "providerAvatarUrl")
    @Mapping(source = "lastSyncAt", target = "lastSyncAt")
    UserConnectionDTO toDTO(UserConnection userConnection);
    
    /**
     * 将 UserConnectionDTO 转换为 UserConnection 实体
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "provider", target = "provider")
    @Mapping(source = "providerUserId", target = "providerUserId")
    @Mapping(source = "providerUsername", target = "providerUsername")
    @Mapping(source = "providerEmail", target = "providerEmail")
    @Mapping(source = "providerAvatarUrl", target = "providerAvatarUrl")
    @Mapping(target = "userId", ignore = true)  // DTO 中通常不包含 userId
    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "tokenExpiresAt", ignore = true)
    @Mapping(target = "extraData", ignore = true)
    @Mapping(target = "isUnbound", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserConnection toEntity(UserConnectionDTO dto);
    
    /**
     * 批量转换：List<UserConnection> -> List<UserConnectionDTO>
     */
    List<UserConnectionDTO> toDTOList(List<UserConnection> userConnections);
    
    /**
     * 批量转换：List<UserConnectionDTO> -> List<UserConnection>
     */
    List<UserConnection> toEntityList(List<UserConnectionDTO> dtos);
}