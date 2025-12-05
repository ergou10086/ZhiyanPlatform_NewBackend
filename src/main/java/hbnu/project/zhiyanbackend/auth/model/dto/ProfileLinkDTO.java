package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人关联链接 DTO
 * 用于在个人信息中展示/保存用户的自定义外部链接
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileLinkDTO {

    /**
     * 展示名称，可选
     */
    private String label;

    /**
     * 链接地址，必须以 http/https 开头
     */
    private String url;
}

