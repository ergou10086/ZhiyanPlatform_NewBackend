package hbnu.project.zhiyanbackend.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2第三方登录控制器
 * 处理OAuth2授权和回调
 *
 * @author ErgouTree
 * @rewrite yui
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2第三方登录", description = "GitHub等第三方登录相关接口")
public class OAuth2Controller {
}
