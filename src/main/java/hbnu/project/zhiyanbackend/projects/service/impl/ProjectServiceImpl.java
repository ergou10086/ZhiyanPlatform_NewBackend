package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementDetailRepository;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementFileRepository;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementDetailsService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import hbnu.project.zhiyanbackend.projects.model.dto.ProjectDTO;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import hbnu.project.zhiyanbackend.projects.repository.ProjectMemberRepository;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.projects.service.ProjectService;
import hbnu.project.zhiyanbackend.projects.model.form.ProjectOwnershipTransferRequest;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import hbnu.project.zhiyanbackend.auth.service.UserService;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.security.utils.PermissionUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;
import hbnu.project.zhiyanbackend.tasks.repository.TaskRepository;
import hbnu.project.zhiyanbackend.wiki.service.WikiContentVersionService;
import hbnu.project.zhiyanbackend.wiki.service.WikiOssService;
import hbnu.project.zhiyanbackend.wiki.service.WikiPageService;
import hbnu.project.zhiyanbackend.wiki.repository.WikiAttachmentRepository;
import hbnu.project.zhiyanbackend.wiki.repository.WikiPageRepository;
import hbnu.project.zhiyanbackend.wiki.repository.WikiVersionHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 项目服务实现类
 * 提供项目的创建、更新、删除、查询等核心业务功能
 * 包括项目管理、成员管理、权限控制等相关功能
 *
 * @author Tokito
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    /**
     * 项目数据访问层
     */
    private final ProjectRepository projectRepository;
    /**
     * 项目成员数据访问层
     */
    private final ProjectMemberRepository projectMemberRepository;
    /**
     * 项目成员服务层
     */
    private final ProjectMemberService projectMemberService;
    /**
     * 消息服务层
     */
    private final InboxMessageService inboxMessageService;
    /**
     * 用户服务层
     */
    private final UserService userService;
    /**
     * 用户数据访问层
     */
    private final UserRepository userRepository;
    /**
     * 任务数据访问层
     */
    private final TaskRepository taskRepository;
    /**
     * 维基页面服务层
     */
    private final WikiPageService wikiPageService;
    /**
     * 维基页面数据访问层
     */
    private final WikiPageRepository wikiPageRepository;
    /**
     * 维基版本历史数据访问层
     */
    private final WikiVersionHistoryRepository wikiPageHistoryRepository;
    /**
     * 维基附件数据访问层
     */
    private final WikiAttachmentRepository wikiAttachmentRepository;
    /**
     * 维基内容版本服务层
     */
    private final WikiContentVersionService wikiContentVersionService;
    /**
     * 维基OSS服务层
     */
    private final WikiOssService wikiOssService;
    /**
     * 成就数据访问层
     */
    private final AchievementRepository achievementRepository;
    /**
     * 成就详情数据访问层
     */
    private final AchievementDetailRepository achievementDetailRepository;
    /**
     * 成就文件数据访问层
     */
    private final AchievementFileRepository achievementFileRepository;
    /**
     * 成就文件服务层
     */
    private final AchievementFileService achievementFileService;
    /**
     * 成就详情服务层
     */
    private final AchievementDetailsService achievementDetailsService;

    /**
     * 创建新项目
     * @param name 项目名称
     * @param description 项目描述
     * @param visibility 项目可见性
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param imageUrl 项目图片URL
     * @param creatorId 创建者ID
     * @return 创建结果，包含项目信息
     */
    @Override
    @Transactional
    @CacheEvict(value = "projectSquare", allEntries = true)
    public R<Project> createProject(String name,
                                    String description,
                                    ProjectVisibility visibility,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String imageUrl,
                                    Long creatorId) {
        try {
            // 验证项目名称非空
            if (!StringUtils.hasText(name)) {
                return R.fail("项目名称不能为空");
            }

            // 验证项目名称唯一性
            if (projectRepository.existsByNameAndIsDeletedFalse(name)) {
                return R.fail("项目名称已存在: " + name);
            }

            // 验证创建者存在
            if (creatorId == null) {
                return R.fail("未登录或令牌无效，无法创建项目");
            }

            if(!userRepository.existsById(creatorId)) {
                return R.fail("棍木不能创建项目");
            }

            // 构建项目实体
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

            // 保存项目信息
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

    /**
     * 更新项目信息
     * @param projectId 项目ID
     * @param name 项目名称
     * @param description 项目描述
     * @param visibility 项目可见性
     * @param status 项目状态
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param imageUrl 项目图片URL
     * @return 更新结果，包含更新后的项目信息
     */
    @Override
    @Transactional
    @CacheEvict(value = "projectSquare", allEntries = true)
    public R<Project> updateProject(Long projectId,
                                    String name,
                                    String description,
                                    ProjectVisibility visibility,
                                    ProjectStatus status,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String imageUrl) {
        try {
            // 查找项目
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            // 更新项目名称（如果提供且与原名称不同）
            if (StringUtils.hasText(name) && !name.equals(project.getName())) {
                if (projectRepository.existsByNameAndIdNotAndIsDeleted(name, projectId, false)) {
                    return R.fail("项目名称已存在: " + name);
                }
                project.setName(name);
            }

            // 更新项目描述
            if (StringUtils.hasText(description)) {
                project.setDescription(description);
            }

            // 更新项目可见性
            if (visibility != null) {
                project.setVisibility(visibility);
            }

            // 更新项目状态
            ProjectStatus oldStatus = project.getStatus();
            if (status != null && status != oldStatus) {
                project.setStatus(status);
            }

            // 更新开始日期
            if (startDate != null) {
                project.setStartDate(startDate);
            }

            // 更新结束日期
            if (endDate != null) {
                project.setEndDate(endDate);
            }

            // 保存更新后的项目信息
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

    /**
     * 删除项目
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 删除结果
     */
    @Override
    @Transactional
    @CacheEvict(value = "projectSquare", allEntries = true)
    public R<Void> deleteProject(Long projectId, Long userId) {
        try {
            // 查找项目
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            // 只有项目拥有者或者系统管理员可以删除
            boolean isOwner = projectMemberService.isOwner(projectId, userId);
            boolean isSystemAdmin = PermissionUtils.hasRole("DEVELOPER");
            if (!(isOwner || isSystemAdmin)) {
                return R.fail("只有项目拥有者或者系统管理员才能删除项目");
            }

            // 执行级联删除
            // TODO: 级联删除实现 performCascadeDeletion(projectId, userId);

            project.setIsDeleted(true);
            projectRepository.save(project);
            
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
            // 填充创建者名称，便于前端展示项目负责人，与 DTO 逻辑保持一致
            String creatorName = "未知用户";
            if (project.getCreatorId() != null) {
                try {
                    creatorName = userRepository.findNameById(project.getCreatorId()).orElse("未知用户");
                } catch (Exception e) {
                    log.warn("查询创建者名称失败: creatorId={}", project.getCreatorId(), e);
                }
            }
            project.setCreatorName(creatorName);
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
            return R.fail("用户参与的项目列表失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getOwnedProjects(Long ownerId, Pageable pageable, String keyword) {
        try {
            if (ownerId == null) {
                return R.fail("未登录或令牌无效，无法获取我拥有的项目");
            }
            Page<Project> projects = projectRepository.findOwnedProjectsByUser(ownerId, keyword, pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("获取用户拥有的项目列表失败: ownerId={}", ownerId, e);
            return R.fail("获取我拥有的项目列表失败: " + e.getMessage());
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
    public R<Long> countUserOwnedProjectsAsOwner(Long userId) {
        try {
            if (userId == null) {
                return R.fail("未登录或令牌无效，无法统计我拥有的项目数量");
            }
            long count = projectRepository.countOwnedProjectsByUser(userId);
            return R.ok(count);
        } catch (Exception e) {
            log.error("统计用户作为OWNER的项目数量失败: userId={}", userId, e);
            return R.fail("统计失败: " + e.getMessage());
        }
    }

    @Override
    public R<Page<Project>> getPublicActiveProjects(Pageable pageable) {
        try {
            Long currentUserId = SecurityUtils.getUserId();
            Page<Project> projects = projectRepository.findPublicActiveProjects(currentUserId, pageable);
            return R.ok(projects);
        } catch (Exception e) {
            log.error("获取公开活跃项目失败", e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "projectSquare", allEntries = true)
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
    @CacheEvict(value = "projectSquare", allEntries = true)
    public R<Void> archiveProject(Long projectId, Long userId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            if (!projectMemberService.isOwner(projectId, userId)) {
                return R.fail("只有项目拥有者才能归档项目");
            }

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
        long start = System.currentTimeMillis();
        try {
            long dbStart = System.currentTimeMillis();
            long count = projectRepository.countByCreatorId(userId);
            long dbCost = System.currentTimeMillis() - dbStart;

            long total = System.currentTimeMillis() - start;
            log.info("[projectCount] created DB={}ms, total={}ms, userId={}", dbCost, total, userId);

            return R.ok(count);
        } catch (Exception e) {
            log.error("统计用户创建项目数量失败: userId={}", userId, e);
            return R.fail("统计失败: " + e.getMessage());
        }
    }

    @Override
    public R<Long> countUserParticipatedProjects(Long userId) {
        long start = System.currentTimeMillis();
        try {
            long dbStart = System.currentTimeMillis();
            long count = projectMemberRepository.countByUserId(userId);
            long dbCost = System.currentTimeMillis() - dbStart;

            long total = System.currentTimeMillis() - start;
            log.info("[projectCount] participated DB={}ms, total={}ms, userId={}", dbCost, total, userId);

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
    @Cacheable(value = "projectSquare",
            key = "T(String).format('page_%d_size_%d_user_%s', #pageable.pageNumber, #pageable.pageSize, T(hbnu.project.zhiyanbackend.security.utils.SecurityUtils).getUserId())")
    public R<Page<ProjectDTO>> getPublicActiveProjectsDTO(Pageable pageable) {
        Long currentUserId = SecurityUtils.getUserId();
        return getPublicActiveProjectsDTO(pageable, currentUserId);
    }

    public R<Page<ProjectDTO>> getPublicActiveProjectsDTO(Pageable pageable, Long currentUserId) {
        long start = System.currentTimeMillis();
        try {
            long dbStart = System.currentTimeMillis();
            Page<Project> projects = projectRepository.findPublicActiveProjects(currentUserId, pageable);
            long dbCost = System.currentTimeMillis() - dbStart;

            long convertStart = System.currentTimeMillis();
            List<ProjectDTO> dtoList = convertToDTOList(projects.getContent(), currentUserId);
            long convertCost = System.currentTimeMillis() - convertStart;

            Page<ProjectDTO> dtoPage = new PageImpl<>(dtoList, pageable, projects.getTotalElements());

            long totalCost = System.currentTimeMillis() - start;
            log.info("[projectSquare] DB={}ms, convert={}ms, total={}ms, page={}, size={}, user={}",
                    dbCost, convertCost, totalCost, pageable.getPageNumber(), pageable.getPageSize(), currentUserId);

            return R.ok(dtoPage);
        } catch (Exception e) {
            log.error("获取公开活跃项目失败", e);
            return R.fail("获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 将Project实体转换为ProjectDTO，并填充创建者名称
     * @param project 项目实体
     * @param currentUserId 当前登录用户ID
     * @return ProjectDTO
     */
    private ProjectDTO convertToDTO(Project project, Long currentUserId) {
        if (project == null) {
            return null;
        }
        
        // 查询创建者名称
        String creatorName = "未知用户";
        if (project.getCreatorId() != null) {
            try {
                creatorName = userRepository.findNameById(project.getCreatorId()).orElse("未知用户");
            } catch (Exception e) {
                log.warn("查询创建者名称失败: creatorId={}", project.getCreatorId(), e);
            }
        }

        Long projectId = project.getId();

        // 查询成员数量
        int memberCount = 0;
        try {
            memberCount = (int) projectMemberRepository.countByProjectId(projectId);
        } catch (Exception e) {
            log.warn("查询项目成员数量失败: projectId={}", project.getId(), e);
        }

        // 查询任务数量
        int taskCount = 0;
        try{
            taskCount = (int) taskRepository.countByProjectIdAndIsDeletedFalse(projectId);
        }catch (Exception e){
            log.warn("查询项目任务数量失败: projectId={}", project.getId(), e);
        }
        
        String accessibleUserId = null;
        if (project.getVisibility() == ProjectVisibility.PRIVATE && currentUserId != null) {
            accessibleUserId = String.valueOf(currentUserId);
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
                .taskCount(taskCount)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .accessibleUserId(accessibleUserId)
                .build();
    }
    
    /**
     * 批量转换Project列表为ProjectDTO列表
     * @param projects 项目列表
     * @param currentUserId 当前登录用户ID
     * @return ProjectDTO列表
     */
    private List<ProjectDTO> convertToDTOList(List<Project> projects, Long currentUserId) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }

        long start = System.currentTimeMillis();
        long userCost = 0L;
        long memberCost = 0L;
        long taskCost = 0L;
        long mapCost;

        List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        List<Long> creatorIds = projects.stream()
                .map(Project::getCreatorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> creatorNameMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            try {
                long t0 = System.currentTimeMillis();
                List<Object[]> rows = userRepository.findIdAndNameByIdInAndIsDeletedFalse(creatorIds);
                userCost = System.currentTimeMillis() - t0;
                for (Object[] row : rows) {
                    Long uid = (Long) row[0];
                    String name = (String) row[1];
                    if (name == null || name.isEmpty()) {
                        name = "未知用户";
                    }
                    creatorNameMap.put(uid, name);
                }
            } catch (Exception e) {
                log.warn("批量查询项目创建者名称失败: creatorIds={}", creatorIds, e);
            }
        }

        Map<Long, Long> memberCountMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            try {
                long t0 = System.currentTimeMillis();
                List<ProjectMember> members = projectMemberRepository.findByProjectIdIn(projectIds);
                memberCost = System.currentTimeMillis() - t0;
                Map<Long, Long> tmpMemberCount = members.stream()
                        .collect(Collectors.groupingBy(ProjectMember::getProjectId, Collectors.counting()));
                memberCountMap.putAll(tmpMemberCount);
            } catch (Exception e) {
                log.warn("批量查询项目成员数量失败: projectIds={}", projectIds, e);
            }
        }

        Map<Long, Long> taskCountMap = new HashMap<>();
        if (!projectIds.isEmpty()) {
            try {
                long t0 = System.currentTimeMillis();
                List<Object[]> taskCounts = taskRepository.countTasksByProjectIds(projectIds);
                taskCost = System.currentTimeMillis() - t0;
                for (Object[] row : taskCounts) {
                    Long pid = (Long) row[0];
                    Long count = (Long) row[1];
                    taskCountMap.put(pid, count);
                }
            } catch (Exception e) {
                log.warn("批量查询项目任务数量失败: projectIds={}", projectIds, e);
            }
        }

        long mapStart = System.currentTimeMillis();
        List<ProjectDTO> result = projects.stream()
                .map(project -> {
                    Long projectId = project.getId();

                    String creatorName = creatorNameMap.getOrDefault(
                            project.getCreatorId(),
                            "未知用户");

                    int memberCount = memberCountMap.getOrDefault(projectId, 0L).intValue();
                    int taskCount = taskCountMap.getOrDefault(projectId, 0L).intValue();

                    String accessibleUserId = null;
                    if (project.getVisibility() == ProjectVisibility.PRIVATE && currentUserId != null) {
                        accessibleUserId = String.valueOf(currentUserId);
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
                            .taskCount(taskCount)
                            .createdAt(project.getCreatedAt())
                            .updatedAt(project.getUpdatedAt())
                            .accessibleUserId(accessibleUserId)
                            .build();
                })
                .toList();
        mapCost = System.currentTimeMillis() - mapStart;

        long total = System.currentTimeMillis() - start;
        log.info("[projectSquare][convertToDTOList] user={}ms, members={}ms, tasks={}ms, map={}ms, total={}ms, size={}",
                userCost, memberCost, taskCost, mapCost, total, projects.size());

        return result;
    }

    /**
     * 保存项目草稿
     * @param name 项目名称
     * @param description 项目描述
     * @param visibility 项目可见性
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param imageUrl 项目图片URL
     * @param creatorId 创建者ID
     * @return 保存结果，包含草稿项目信息
     */
    @Override
    @Transactional
    public R<Project> saveDraft(String name,
                                String description,
                                ProjectVisibility visibility,
                                LocalDate startDate,
                                LocalDate endDate,
                                String imageUrl,
                                Long creatorId) {
        try {
            // 验证创建者存在
            if (creatorId == null) {
                return R.fail("未登录或令牌无效，无法保存草稿");
            }

            if (!userRepository.existsById(creatorId)) {
                return R.fail("用户不存在，无法保存草稿");
            }

            // 查找用户是否已有草稿
            Optional<Project> existingDraft = projectRepository.findByCreatorIdAndIsDraftTrueAndIsDeletedFalse(creatorId);

            Project draft;
            if (existingDraft.isPresent()) {
                // 更新现有草稿
                draft = existingDraft.get();
                if (StringUtils.hasText(name)) {
                    draft.setName(name);
                }
                if (description != null) {
                    draft.setDescription(description);
                }
                if (visibility != null) {
                    draft.setVisibility(visibility);
                }
                if (startDate != null) {
                    draft.setStartDate(startDate);
                }
                if (endDate != null) {
                    draft.setEndDate(endDate);
                }
                if (imageUrl != null) {
                    draft.setImageUrl(imageUrl);
                }
                draft.setIsDraft(true);
            } else {
                // 创建新草稿
                draft = Project.builder()
                        .name(StringUtils.hasText(name) ? name : "未命名项目")
                        .description(description)
                        .status(ProjectStatus.PLANNING)
                        .visibility(visibility != null ? visibility : ProjectVisibility.PRIVATE)
                        .startDate(startDate)
                        .endDate(endDate)
                        .imageUrl(imageUrl)
                        .creatorId(creatorId)
                        .isDeleted(false)
                        .isDraft(true)
                        .build();

                // 显式设置审计创建人
                draft.setCreatedBy(creatorId);
            }

            // 保存草稿
            draft = projectRepository.save(draft);

            log.info("保存项目草稿成功: id={}, name={}, creatorId={}", draft.getId(), draft.getName(), creatorId);
            return R.ok(draft, "草稿保存成功");
        } catch (Exception e) {
            log.error("保存项目草稿失败: name={}, creatorId={}", name, creatorId, e);
            return R.fail("保存草稿失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Void> transferOwnership(Long projectId, Long newOwnerId, Long operatorId) {
        try {
            if (projectId == null || newOwnerId == null || operatorId == null) {
                return R.fail("项目ID、新拥有者ID和操作者ID不能为空");
            }

            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null || Boolean.TRUE.equals(project.getIsDeleted())) {
                return R.fail("项目不存在或已被删除");
            }

            // 只有当前项目OWNER才能移交所有权
            if (!projectMemberService.isOwner(projectId, operatorId)) {
                return R.fail("只有当前项目负责人可以移交项目所有权");
            }

            // 新拥有者必须是项目成员
            if (!projectMemberService.isMember(projectId, newOwnerId)) {
                return R.fail("新的项目负责人必须是该项目的成员");
            }

            // 查找当前OWNER成员记录
            Optional<ProjectMember> currentOwnerOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, operatorId);
            if (currentOwnerOpt.isEmpty()) {
                return R.fail("未找到当前项目负责人的成员记录");
            }

            // 查找新的OWNER成员记录
            Optional<ProjectMember> newOwnerMemberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, newOwnerId);
            if (newOwnerMemberOpt.isEmpty()) {
                return R.fail("新的项目负责人不是项目成员");
            }

            ProjectMember currentOwner = currentOwnerOpt.get();
            ProjectMember newOwnerMember = newOwnerMemberOpt.get();

            if (newOwnerMember.getProjectRole() == ProjectMemberRole.OWNER) {
                // 目标用户已经是OWNER，则视为成功
                log.info("项目所有权移交请求，但目标用户已是OWNER: projectId={}, operatorId={}, newOwnerId={}",
                        projectId, operatorId, newOwnerId);
                return R.ok();
            }

            // 将当前OWNER降级为普通成员，将目标用户升级为OWNER
            currentOwner.setProjectRole(ProjectMemberRole.MEMBER);
            newOwnerMember.setProjectRole(ProjectMemberRole.OWNER);
            projectMemberRepository.save(currentOwner);
            projectMemberRepository.save(newOwnerMember);

            // 更新项目的creatorId和审计字段，保证前端展示负责人不会变成“未知用户”
            project.setCreatorId(newOwnerId);
            project.setCreatedBy(newOwnerId);
            projectRepository.save(project);

            log.info("项目所有权移交成功: projectId={}, oldOwnerId={}, newOwnerId={}",
                    projectId, operatorId, newOwnerId);
            return R.ok();
        } catch (Exception e) {
            log.error("项目所有权移交失败: projectId={}, operatorId={}, newOwnerId={}",
                    projectId, operatorId, newOwnerId, e);
            return R.fail("项目所有权移交失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Void> transferOwnershipBatch(List<ProjectOwnershipTransferRequest> transfers, Long operatorId) {
        try {
            if (operatorId == null) {
                return R.fail("未登录或令牌无效，无法批量移交项目所有权");
            }
            if (transfers == null || transfers.isEmpty()) {
                return R.fail("移交列表不能为空");
            }

            for (ProjectOwnershipTransferRequest req : transfers) {
                if (req == null || req.getProjectId() == null || req.getNewOwnerId() == null) {
                    return R.fail("移交列表中存在无效的项目或新负责人ID");
                }
                R<Void> result = transferOwnership(req.getProjectId(), req.getNewOwnerId(), operatorId);
                if (!R.isSuccess(result)) {
                    // 一旦某个项目移交失败，直接返回该错误
                    return result;
                }
            }

            log.info("批量项目所有权移交成功: operatorId={}, count={}", operatorId, transfers.size());
            return R.ok();
        } catch (Exception e) {
            log.error("批量项目所有权移交失败: operatorId={}", operatorId, e);
            return R.fail("批量项目所有权移交失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的草稿项目
     * @param userId 用户ID
     * @return 返回草稿项目信息，如果不存在则返回空
     */
    @Override
    public R<Project> getDraft(Long userId) {
        try {
            if (userId == null) {
                return R.fail("未登录或令牌无效，无法获取草稿");
            }

            Optional<Project> draft = projectRepository.findByCreatorIdAndIsDraftTrueAndIsDeletedFalse(userId);

            return draft.map(project -> R.ok(project, "获取草稿成功")).orElseGet(() -> R.ok(null, "暂无草稿"));
        } catch (Exception e) {
            log.error("获取项目草稿失败: userId={}", userId, e);
            return R.fail("获取草稿失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户的草稿项目
     * @param userId 用户ID
     * @return 返回操作结果
     */
    @Override
    @Transactional
    public R<Void> deleteDraft(Long userId) {
        try {
            if (userId == null) {
                return R.fail("未登录或令牌无效，无法删除草稿");
            }

            Optional<Project> draft = projectRepository.findByCreatorIdAndIsDraftTrueAndIsDeletedFalse(userId);
            
            if (draft.isPresent()) {
                // 软删除草稿
                Project project = draft.get();
                project.setIsDeleted(true);
                projectRepository.save(project);
                log.info("删除项目草稿成功: id={}, creatorId={}", project.getId(), userId);
                return R.ok(null, "草稿删除成功");
            } else {
                return R.ok(null, "草稿不存在");
            }
        } catch (Exception e) {
            log.error("删除项目草稿失败: userId={}", userId, e);
            return R.fail("删除草稿失败: " + e.getMessage());
        }
    }
}

