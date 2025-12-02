package hbnu.project.zhiyanbackend.wiki.controller;

import hbnu.project.zhiyanbackend.wiki.service.WikiContentVersionService;
import hbnu.project.zhiyanbackend.wiki.service.WikiPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wiki页面控制器
 * 提供Wiki页面的CRUD和权限控制
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki")
@RequiredArgsConstructor
public class WikiPageController {

    private final WikiPageService wikiPageService;

    private final WikiContentVersionService contentVersionService;


}
