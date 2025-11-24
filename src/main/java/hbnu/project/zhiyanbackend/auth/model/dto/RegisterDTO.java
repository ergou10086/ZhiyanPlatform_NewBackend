package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.*;

/**
 * 用户注册 DTO
 *
 * @author ErgouTree
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {

    private String email;

    private String password;

    private String captcha;
}
