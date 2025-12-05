package hbnu.project.zhiyanbackend.auth.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新个人关联链接请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileLinksDTO {

    /**
     * 链接列表，最多6条
     */
    @NotNull(message = "关联链接列表不能为空")
    @Size(max = 6, message = "关联链接最多6个")
    @Valid
    private List<ProfileLinkDTO> links;
}

