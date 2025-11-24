package hbnu.project.zhiyanbackend.auth.model.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * 用户信息更新数据传输对象
 * 非验证类的在这里修改，如果有需要可以限制时间
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {

    /**
     * 用户姓名
     */
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String name;

    /**
     * 用户职称/职位
     */
    @Size(max = 100, message = "职称/职位长度不能超过100个字符")
    private String title;

    /**
     * 所属机构
     */
    @Size(max = 200, message = "所属机构长度不能超过200个字符")
    private String institution;

    /**
     * 头像Base64编码数据（可选）
     * 格式：data:image/jpeg;base64,/9j/4AAQSkZJRg... 或直接 base64 字符串
     */
    private String avatarData;

    /**
     * 头像MIME类型（可选，如果提供avatarData但未提供此字段，将从avatarData中解析）
     */
    @Size(max = 50, message = "MIME类型长度不能超过50个字符")
    private String avatarContentType;

    /**
     * 账号是否锁定
     */
    private Boolean isLocked;

    /**
     * 用户角色ID列表
     */
    private List<Long> roleIds;
}