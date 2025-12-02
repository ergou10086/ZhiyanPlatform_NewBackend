package hbnu.project.zhiyanbackend.wiki.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Wiki导入导出控制器
 * 提供Wiki页面的导入导出功能
 *
 * @author Tokito
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/wiki")     // 原 /api/wiki
@RequiredArgsConstructor
@Tag(name = "Wiki导入导出", description = "Wiki页面导入导出相关接口")
public class WikiImportExportController {


}

