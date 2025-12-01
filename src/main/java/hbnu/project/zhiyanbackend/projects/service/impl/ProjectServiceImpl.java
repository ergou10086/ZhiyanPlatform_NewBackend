package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.dto.ProjectDTO;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import hbnu.project.zhiyanbackend.projects.repository.ProjectMemberRepository;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.projects.service.ProjectService;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.auth.service.UserService;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目服务实现
 *
 * @author Tokito
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberService projectMemberService;
    private final InboxMessageService inboxMessageService;
    private final UserService userService;

    @Override
    @Transactional
    public R<Project> createProject(String name,
                                    String description,
                                    ProjectVisibility visibility,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String imageUrl,
                                    Long creatorId) {
        try {
            if (!StringUtils.hasText(name)) {
                return R.fail("项目名称不能为空");
            }

            if (projectRepository.existsByNameAndIsDeletedFalse(name)) {
                return R.fail("项目名称已存在: " + name);
            }

            if (creatorId == null) {
                return R.fail("未登录或令牌无效，无法创建项目");
            }

            // TODO 后续接入认证模块时，可以在这里根据 creatorId 加载并校验用户信息

            Project project = Project.builder()
                    .name(name)
                    .description(description)
                    .status(ProjectStatus.PLANNING)
                    .visibility(visibility != null ? visibility : ProjectVisibility.PRIVATE)
                    .startDate(startDate)
                    .endDate(endDate)
                    .creatorId(creatorId)
                    .isDeleted(false)
                    .build();

            // 显式设置审计创建人，避免约束问题
            project.setCreatedBy(creatorId);

            project = projectRepository.save(project);
            
            // 创建者作为项目拥有者加入成员表
            projectMemberService.addMemberInternal(project.getId(), creatorId, ProjectMemberRole.OWNER);

            // 发送项目创建消息（通知创建者）
            try {
                inboxMessageService.sendPersonalMessage(
                        MessageScene.PROJECT_CREATED,
                        creatorId,
                        creatorId,
                        "项目创建成功",
                        String.format("您已成功创建项目「%s」", project.getName()),
                        project.getId(),
                        "PROJECT",
                        null
                );
            } catch (Exception e) {
                log.warn("发送项目创建消息失败: projectId={}, creatorId={}", project.getId(), creatorId, e);
            }

            log.info("创建项目成功: id={}, name={}, creatorId={}", project.getId(), name, creatorId);
            return R.ok(project, "项目创建成功");
        } catch (Exception e) {
            log.error("创建项目失败: name={}, creatorId={}", name, creatorId, e);
            return R.fail("项目创建失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Project> updateProject(Long projectId,
                                    String name,
                                    String description,
                                    ProjectVisibility visibility,
                                    ProjectStatus status,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String imageUrl) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            if (StringUtils.hasText(name) && !name.equals(project.getName())) {
                if (projectRepository.existsByNameAndIdNotAndIsDeleted(name, projectId, false)) {
                    return R.fail("项目名称已存在: " + name);
                }
                project.setName(name);
            }

            if (StringUtils.hasText(description)) {
                project.setDescription(description);
            }

            if (visibility != null) {
                project.setVisibility(visibility);
            }

            ProjectStatus oldStatus = project.getStatus();
            if (status != null && status != oldStatus) {
                project.setStatus(status);
            }

            if (startDate != null) {
                project.setStartDate(startDate);
            }

            if (endDate != null) {
                project.setEndDate(endDate);
            }

            project = projectRepository.save(project);
            
            // 如果项目状态发生变更，发送状态变更消息给所有项目成员
            if (status != null && status != oldStatus) {
                try {
                    List<Long> memberIds = projectMemberService.getProjectMemberUserIds(projectId);
                    if (!memberIds.isEmpty()) {
                        inboxMessageService.sendBatchPersonalMessage(
                                MessageScene.PROJECT_STATUS_CHANGED,
                                project.getCreatedBy(),
                                memberIds,
                                "项目状态已变更",
                                String.format("项目「%s」的状态已从「%s」变更为「%s」", 
                                        project.getName(), oldStatus, status),
                                project.getId(),
                                "PROJECT",
                                null
                        );
                    }
                } catch (Exception e) {
                    log.warn("发送项目状态变更消息失败: projectId={}", projectId, e);
                }
            }
            
            log.info("更新项目成功: id={}, name={}", projectId, project.getName());
            return R.ok(project, "项目更新成功");
        } catch (Exception e) {
            log.error("更新项目失败: projectId={}", projectId, e);
            return R.fail("项目更新失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Void> deleteProject(Long projectId, Long userId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            // 只有项目拥有者可以删除
            if (!projectMemberService.isOwner(projectId, userId)) {
                return R.fail("只有项目拥有者才能删除项目");
            }

            // TODO 后续接入认证/权限模块时，可在此扩展更多删除权限规则（如系统管理员强制删除）

            project.setIsDeleted(true);
            projectRepository.save(project);

            // TODO:项目删除时候，需要级联删除项目内对应的所有内容，包括任务，知识库（记录和文件），整个的wiki文档
            
            // 发送项目删除消息给所有项目成员
            try {
                List<Long> memberIds = projectMemberService.getProjectMemberUserIds(projectId);
                if (!memberIds.isEmpty()) {
                    inboxMessageService.sendBatchPersonalMessage(
                            MessageScene.PROJECT_DELETED,
                            userId,
                            memberIds,
                            "项目已删除",
                            String.format("项目「%s」已被删除", project.getName()),
                            project.getId(),
                            "PROJECT",
                            null
                    );
                }
            } catch (Exception e) {
                log.warn("发送项目删除消息失败: projectId={}", projectId, e);
            }

            log.info("软删除项目成功: id={}, operator={} ", projectId, userId);
            return R.ok(null, "项目删除成功");
        } catch (Exception e) {
            log.error("删除项目失败: projectId={}, userId={}", projectId, userId, e);
            return R.fail("项目删除失败: " + e.getMessage());
        }
    }

    @Override
    public R<Project> getProjectById(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }
            return R.ok(project);
        } catch (Exception e) {
            log.error("获取项目失败: projectId={}", projectId, e);
            return R.fail("获取项目失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getAllProjects(Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.findAllActive(pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("获取项目列表失败", e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getProjectsByCreator(Long creatorId, Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.findByCreatorIdAndIsDeleted(creatorId, false, pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("获取用户创建项目列表失败: creatorId={}", creatorId, e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getProjectsByStatus(ProjectStatus status, Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.findByStatusAndIsDeleted(status, false, pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("按状态获取项目列表失败: status={}", status, e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getUserProjects(Long userId, Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.findUserProjects(userId, pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("获取用户参与的项目列表失败: userId={}", userId, e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> searchProjects(String keyword, Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.searchByKeyword(keyword, pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("搜索项目失败: keyword={}", keyword, e);
            return R.fail("搜索项目失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getPublicActiveProjects(Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.findPublicActiveProjects(pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("获取公开活跃项目失败", e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Project> updateProjectStatus(Long projectId, ProjectStatus status) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }
            ProjectStatus oldStatus = project.getStatus();
            project.setStatus(status);
            project = projectRepository.save(project);
            
            // 发送项目状态变更消息给所有项目成员
            if (status != oldStatus) {
                try {
                    List<Long> memberIds = projectMemberService.getProjectMemberUserIds(projectId);
                    if (!memberIds.isEmpty()) {
                        inboxMessageService.sendBatchPersonalMessage(
                                MessageScene.PROJECT_STATUS_CHANGED,
                                project.getCreatedBy(),
                                memberIds,
                                "项目状态已变更",
                                String.format("项目「%s」的状态已从「%s」变更为「%s」", 
                                        project.getName(), oldStatus, status),
                                project.getId(),
                                "PROJECT",
                                null
                        );
                    }
                } catch (Exception e) {
                    log.warn("发送项目状态变更消息失败: projectId={}", projectId, e);
                }
            }
            
            log.info("更新项目状态成功: id={}, status={}", projectId, status);
            return R.ok(project, "项目状态更新成功");
        } catch (Exception e) {
            log.error("更新项目状态失败: projectId={}, status={}", projectId, status, e);
            return R.fail("更新项目状态失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Void> archiveProject(Long projectId, Long userId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            if (!projectMemberService.isOwner(projectId, userId)) {
                return R.fail("只有项目拥有者才能归档项目");
            }

            // TODO 后续接入认证/权限模块时，可在此扩展更多归档权限规则

            project.setStatus(ProjectStatus.ARCHIVED);
            projectRepository.save(project);
            
            // 发送项目归档消息给所有项目成员
            try {
                List<Long> memberIds = projectMemberService.getProjectMemberUserIds(projectId);
                if (!memberIds.isEmpty()) {
                    inboxMessageService.sendBatchPersonalMessage(
                            MessageScene.PROJECT_ARCHIVED,
                            userId,
                            memberIds,
                            "项目已归档",
                            String.format("项目「%s」已被归档", project.getName()),
                            project.getId(),
                            "PROJECT",
                            null
                    );
                }
            } catch (Exception e) {
                log.warn("发送项目归档消息失败: projectId={}", projectId, e);
            }

            log.info("归档项目成功: id={}, operator={}", projectId, userId);
            return R.ok(null, "项目归档成功");
        } catch (Exception e) {
            log.error("归档项目失败: projectId={}, userId={}", projectId, userId, e);
            return R.fail("项目归档失败: " + e.getMessage());
        }
    }

    @Override
    public R<Boolean> hasAccessPermission(Long projectId, Long userId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.ok(false);
            }

            if (project.getVisibility() == ProjectVisibility.PUBLIC) {
                return R.ok(true);
            }

            boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
            return R.ok(isMember);
        } catch (Exception e) {
            log.error("检查访问权限失败: projectId={}, userId={}", projectId, userId, e);
            return R.fail("检查访问权限失败: " + e.getMessage());
        }
    }

    @Override
    public R<Long> countUserCreatedProjects(Long userId) {
        try {
            long count = projectRepository.countByCreatorId(userId);
            return R.ok(count);
        } catch (Exception e) {
            log.error("统计用户创建项目数量失败: userId={}", userId, e);
            return R.fail("统计失败: " + e.getMessage());
        }
    }

    @Override
    public R<Long> countUserParticipatedProjects(Long userId) {
        try {
            long count = projectMemberRepository.countByUserId(userId);
            return R.ok(count);
        } catch (Exception e) {
            log.error("统计用户参与项目数量失败: userId={}", userId, e);
            return R.fail("统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取公开且活跃的项目（返回DTO，包含创建者名称）
     * @param pageable 分页参数
     * @return 项目DTO分页列表
     */
    public R<Page<ProjectDTO>> getPublicActiveProjectsDTO(Pageable pageable) {
        try {
            Page<Project> projects = projectRepository.findPublicActiveProjects(pageable);
            List<ProjectDTO> dtoList = convertToDTOList(projects.getContent());
            Page<ProjectDTO> dtoPage = new PageImpl<>(dtoList, pageable, projects.getTotalElements());
            return R.ok(dtoPage);
        } catch (Exception e) {
            log.error("获取公开活跃项目失败", e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 将Project实体转换为ProjectDTO，并填充创建者名称
     * @param project 项目实体
     * @return ProjectDTO
     */
    private ProjectDTO convertToDTO(Project project) {
        if (project == null) {
            return null;
        }
        
        // 查询创建者名称
        String creatorName = "未知用户";
        if (project.getCreatorId() != null) {
            try {
                R<UserDTO> userResult = userService.getCurrentUser(project.getCreatorId());
                if (R.isSuccess(userResult) && userResult.getData() != null) {
                    creatorName = userResult.getData().getName();
                }
            } catch (Exception e) {
                log.warn("查询创建者名称失败: creatorId={}", project.getCreatorId(), e);
            }
        }
        
        // 查询成员数量
        Integer memberCount = 0;
        try {
            memberCount = (int) projectMemberRepository.countByProjectId(project.getId());
        } catch (Exception e) {
            log.warn("查询项目成员数量失败: projectId={}", project.getId(), e);
        }
        
        return ProjectDTO.builder()
                .id(String.valueOf(project.getId()))
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .visibility(project.getVisibility())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .imageUrl(project.getImageUrl())
                .creatorId(String.valueOf(project.getCreatorId()))
                .creatorName(creatorName)
                .memberCount(memberCount)
                .taskCount(0) // TODO: 后续接入任务模块时填充
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
    
    /**
     * 批量转换Project列表为ProjectDTO列表
     * @param projects 项目列表
     * @return ProjectDTO列表
     */
    private List<ProjectDTO> convertToDTOList(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }
        return projects.stream()
                .map(this::convertToDTO)
                .toList();
    }
}

