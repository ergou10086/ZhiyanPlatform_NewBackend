package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户基本信息内部类
 * 用于投影查询结果
 *
 * @author ErgouTree
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserBasicInfo {

    private Long id;

    private String name;

    private String email;

    private String avatarUrl;
}
