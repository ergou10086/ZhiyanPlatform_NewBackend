package hbnu.project.zhiyanbackend.projects.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.dto.ProjectDTO;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import hbnu.project.zhiyanbackend.projects.model.form.CreateProjectRequest;
import hbnu.project.zhiyanbackend.projects.model.form.UpdateProjectRequest;
import hbnu.project.zhiyanbackend.projects.model.form.UpdateProjectStatusRequest;
import hbnu.project.zhiyanbackend.projects.service.ProjectImageService;
import hbnu.project.zhiyanbackend.projects.service.ProjectService;
import hbnu.project.zhiyanbackend.projects.service.impl.ProjectServiceImpl;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目控制器
 * 基于角色的权限控制
 *
 * @author Tokito
 */
@RestController
@RequestMapping("/zhiyan/projects")
@Tag(name = "项目管理", description = "项目增删改查接口（精简版）")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectImageService projectImageService;

    @PostMapping
    @Operation(summary = "创建项目")
    public R<Project> createProject(@RequestBody CreateProjectRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法创建项目");
        }

        ProjectVisibility visibility = request.getVisibility();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        // 当前版本不处理图片，imageUrl 传 null
        return projectService.createProject(request.getName(),
                request.getDescription(),
                visibility,
                startDate,
                endDate,
                null,
                userId);
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "更新项目信息")
    public R<Project> updateProject(@PathVariable("projectId") Long projectId,
                                    @RequestBody UpdateProjectRequest request) {
        ProjectVisibility visibility = request.getVisibility();
        ProjectStatus status = request.getStatus();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        return projectService.updateProject(projectId,
                request.getName(),
                request.getDescription(),
                visibility,
                status,
                startDate,
                endDate,
                null);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "删除项目（软删除）")
    public R<Void> deleteProject(@PathVariable("projectId") Long projectId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法删除项目");
        }
        return projectService.deleteProject(projectId, userId);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "根据ID获取项目详情")
    public R<Project> getProject(@PathVariable("projectId") Long projectId) {
        return projectService.getProjectById(projectId);
    }

    @GetMapping
    @Operation(summary = "分页获取所有项目")
    public R<Page<Project>> getAllProjects(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectService.getAllProjects(pageable);
    }

    @GetMapping("/creator/{creatorId}")
    @Operation(summary = "分页获取指定用户创建的项目")
    public R<Page<Project>> getProjectsByCreator(@PathVariable("creatorId") Long creatorId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectService.getProjectsByCreator(creatorId, pageable);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "根据状态分页获取项目")
    public R<Page<Project>> getProjectsByStatus(@PathVariable("status") ProjectStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectService.getProjectsByStatus(status, pageable);
    }

    @GetMapping("/my-projects")
    @Operation(summary = "分页获取当前用户参与的项目")
    public R<Page<Project>> getMyProjects(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法获取我的项目");
        }

        Pageable pageable = PageRequest.of(page, size);
        return projectService.getUserProjects(userId, pageable);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索项目")
    public R<Page<Project>> searchProjects(@RequestParam("keyword") String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectService.searchProjects(keyword, pageable);
    }

    @GetMapping("/public/active")
    @Operation(summary = "获取公开且活跃的项目")
    public R<Page<ProjectDTO>> getPublicActiveProjects(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        // 使用ProjectServiceImpl的getPublicActiveProjectsDTO方法，返回包含创建者名称的DTO
        return ((ProjectServiceImpl) projectService).getPublicActiveProjectsDTO(pageable);
    }

    @PatchMapping("/{projectId}/status")
    @Operation(summary = "更新项目状态")
    public R<Project> updateProjectStatus(@PathVariable("projectId") Long projectId,
                                          @RequestBody UpdateProjectStatusRequest request) {
        return projectService.updateProjectStatus(projectId, request.getStatus());
    }

    @PostMapping(path = "/{projectId}/image", consumes = "multipart/form-data")
    @Operation(summary = "上传或更新项目封面图片")
    public R<Void> uploadProjectImage(@PathVariable("projectId") Long projectId,
                                      @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return R.fail("上传文件为空");
            }
            // 委托给项目图片服务，内部负责上传到 COS 并更新数据库字段
            return projectImageService.updateProjectImage(projectId, file);
        } catch (Exception e) {
            return R.fail("上传项目图片失败: " + e.getMessage());
        }
    }

    @GetMapping("/{projectId}/image")
    @Operation(summary = "获取项目封面图片")
    public ResponseEntity<byte[]> getProjectImage(@PathVariable("projectId") Long projectId) {
        R<byte[]> result = projectImageService.getProjectImage(projectId);
        if (!R.isSuccess(result) || result.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        // 目前未在数据库中保存具体类型，这里暂时使用通用二进制类型
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(result.getData(), headers, HttpStatus.OK);
    }

    /**
     * 兼容前端使用的上传项目图片接口
     * 前端调用：POST /zhiyan/projects/upload-image
     * 表单字段：file（必填），projectId（必填）
     * 返回：R<{ imageUrl: string }>
     */
    @PostMapping(path = "/upload-image", consumes = "multipart/form-data")
    @Operation(summary = "上传项目封面图片（兼容旧前端接口）")
    public R<Map<String, String>> uploadProjectImageCompatible(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId) {
        try {
            if (projectId == null) {
                return R.fail("项目ID不能为空");
            }

            R<Void> updateResult = projectImageService.updateProjectImage(projectId, file);
            if (!R.isSuccess(updateResult)) {
                return R.fail(updateResult.getMsg());
            }

            // 使用后端图片获取接口构造可访问的URL
            String imageUrl = "/zhiyan/projects/get-image?projectId=" + projectId;
            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", imageUrl);
            return R.ok(data);
        } catch (Exception e) {
            return R.fail("上传项目图片失败: " + e.getMessage());
        }
    }

    /**
     * 兼容前端使用的根据项目ID获取图片接口
     * 前端可通过 imageUrl=/zhiyan/projects/get-image?projectId={id} 直接访问
     */
    @GetMapping("/get-image")
    @Operation(summary = "获取项目封面图片（兼容旧前端接口）")
    public ResponseEntity<byte[]> getProjectImageByQuery(@RequestParam("projectId") Long projectId) {
        return getProjectImage(projectId);
    }

    @GetMapping("/count/my-created")
    @Operation(summary = "统计我创建的项目数量")
    public R<Long> countMyCreatedProjects() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法统计我创建的项目数量");
        }
        return projectService.countUserCreatedProjects(userId);
    }

    @GetMapping("/count/my-participated")
    @Operation(summary = "统计我参与的项目数量")
    public R<Long> countMyParticipatedProjects() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail("未登录或Token无效，无法统计我参与的项目数量");
        }
        return projectService.countUserParticipatedProjects(userId);
    }
}
