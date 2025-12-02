package hbnu.project.zhiyanbackend.wiki.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wiki内容控制器
 * 提供Wiki内容版本管理的相关接口
 *
 * @author Tokito
 * @rewrite ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki/content")     // 原 /api/wiki/content
@RequiredArgsConstructor
@Tag(name = "Wiki内容管理", description = "Wiki内容版本管理相关接口")
public class WikiContentController {
}
