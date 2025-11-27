package hbnu.project.zhiyanbackend.auth.model.dto;

import hbnu.project.zhiyanbackend.auth.model.enums.UserStatus;
import lombok.*;

import java.util.List;

/**
 * 用户信息数据传输对象
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 头像Base64编码数据（可选，如果为空则不返回头像数据）
     */
    private String avatarData;

    /**
     * 头像MIME类型
     */
    private String avatarContentType;

    /**
     * 头像文件大小（字节）
     */
    private Long avatarSize;

    /**
     * 用户职称/职位
     */
    private String title;

    /**
     * 所属机构
     */
    private String institution;

    /**
     * 账号是否锁定
     */
    private Boolean isLocked;

    /**
     * 用户状态
     */
    private UserStatus status;

    /**
     * 用户角色列表
     */
    private List<String> roles;

    /**
     * 用户权限列表
     */
    private List<String> permissions;

    /**
     * 研究方向标签
     */
    private List<String> researchTags;
}