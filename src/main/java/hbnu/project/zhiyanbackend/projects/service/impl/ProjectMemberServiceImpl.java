package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.dto.ProjectMemberDetailDTO;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.repository.ProjectMemberRepository;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 项目成员服务实现（精简版）
 *
 * 本类实现了项目成员相关的业务逻辑，包括添加、移除、更新成员角色等功能，
 * 并提供了查询项目成员信息、判断成员权限等方法。
 *
 * @author Tokito
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    // 依赖注入的仓储和服务
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final InboxMessageService inboxMessageService;
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * 内部添加项目成员
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param role 成员角色
     * @return 新建的项目成员实体
     * @throws IllegalArgumentException 当用户或项目不存在时抛出
     * @throws IllegalStateException 当项目已归档时抛出
     */
    @Override
    @Transactional
    public ProjectMember addMemberInternal(Long projectId, Long userId, ProjectMemberRole role) {
        // 检查用户是否存在
        if(!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("用户不存在，不能被邀请");
        }

        // 检查项目是否存在，获取项目实体
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        // 检查项目状态，已归档项目不能添加成员
        if (project.getStatus() == hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus.ARCHIVED) {
            throw new IllegalStateException("项目已归档，禁止新增或修改成员");
        }

        // 检查用户是否已是项目成员
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("用户[{}]已经是项目[{}]成员", userId, projectId);
            return projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        }

        // 创建新成员实体
        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(userId)
                .projectRole(role)
                .joinedAt(LocalDateTime.now())
                .build();

        // 保存成员信息
        ProjectMember saved = projectMemberRepository.save(member);
        log.info("内部添加项目成员成功: projectId={}, userId={}, role={}", projectId, userId, role);
        return saved;
    }

    /**
     * 添加项目成员对外接口
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param role 成员角色
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> addMember(Long projectId, Long userId, ProjectMemberRole role) {
        try {
            // 检查用户是否存在
            if(!userRepository.existsById(userId)) {
                return R.fail("用户不存在，不能被邀请");
            }

            // 检查项目是否存在
            Project projectEntity = projectRepository.findById(projectId)
                    .orElse(null);
            if (projectEntity == null) {
                return R.fail("项目不存在");
            }
            // 检查项目状态
            if (projectEntity.getStatus() == hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus.ARCHIVED) {
                return R.fail("项目已归档，禁止邀请或修改成员");
            }

            // 检查用户是否已是项目成员
            if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
                return R.fail("该用户已经是项目成员");
            }

            // 创建新成员实体
            ProjectMember member = ProjectMember.builder()
                    .projectId(projectId)
                    .userId(userId)
                    .projectRole(role)
                    .joinedAt(LocalDateTime.now())
                    .build();

            // 保存成员信息
            projectMemberRepository.save(member);

            log.info("添加项目成员成功: projectId={}, userId={}, role={}", projectId, userId, role);
            return R.ok();
        } catch (Exception e) {
            log.error("添加项目成员失败: projectId={}, userId={}, role={}", projectId, userId, role, e);
            return R.fail("添加成员失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目管理员 ID 列表：
     * 申请时就可以一次性拿到需要抄送的管理员列表
     *
     * @param projectId 项目id
     * @return 项目管理员ID列表
     */
    @Override
    public List<Long> getProjectAdminUserIds(Long projectId) {
        // 查找项目负责人
        List<ProjectMember> owners = projectMemberRepository
                .findByProjectIdAndProjectRole(projectId, ProjectMemberRole.OWNER);
        // 查找项目管理员
        List<ProjectMember> admins = projectMemberRepository
                .findByProjectIdAndProjectRole(projectId, ProjectMemberRole.ADMIN);

        // 合并并去重
        return Stream.concat(owners.stream(), admins.stream())
                .map(ProjectMember::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 移除项目成员
     *
     * @param projectId 项目ID
     * @param userId 要移除的用户ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> removeMember(Long projectId, Long userId) {
        try {
            // 查找要移除的成员
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElse(null);
            if (member == null) {
                return R.fail("该用户不是项目成员");
            }

            // TODO 后续接入认证/权限模块时，可在此增加删除成员的权限校验（如仅 OWNER/ADMIN 可移除他人）

            // 在删除前获取所有成员ID（包括即将被移除的成员）
            List<Long> allMemberIds = getProjectMemberUserIds(projectId);
            
            // 删除成员
            projectMemberRepository.delete(member);
            
            // 向所有项目成员发送成员移除消息
            try {
                Project project = projectRepository.findById(projectId).orElse(null);
                if (project != null && !allMemberIds.isEmpty()) {
                    inboxMessageService.sendBatchPersonalMessage(
                            MessageScene.PROJECT_MEMBER_REMOVED,
                            null, // 系统消息
                            allMemberIds,
                            "成员离开项目",
                            String.format("有成员已离开项目「%s」", project.getName()),
                            projectId,
                            "PROJECT",
                            null
                    );
                }
            } catch (Exception e) {
                log.warn("发送项目成员移除消息失败: projectId={}, userId={}", projectId, userId, e);
            }

            log.info("移除项目成员成功: projectId={}, userId={}", projectId, userId);
            return R.ok();
        } catch (Exception e) {
            log.error("移除项目成员失败: projectId={}, userId={}", projectId, userId, e);
            return R.fail("移除成员失败: " + e.getMessage());
        }
    }

    /**
     * 移除项目成员（带操作者信息）
     *
     * @param projectId 项目ID
     * @param userId 要移除的用户ID
     * @param operatorId 操作者ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> removeMember(Long projectId, Long userId, Long operatorId) {
        try {
            // 检查项目是否存在
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

            // 检查操作者是否有权限
            if (!isAdmin(projectId, operatorId)) {
                return R.fail("只有项目管理员可以移除成员");
            }

            // 防止移除自己
            if (userId.equals(operatorId)) {
                return R.fail("不能移除自己，如需退出项目请使用退出功能");
            }

            // 查找要移除的成员
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElse(null);
            if (member == null) {
                return R.fail("该用户不是项目成员");
            }

            // 不能移除项目负责人
            if (member.getProjectRole() == ProjectMemberRole.OWNER) {
                return R.fail("不能移除项目负责人");
            }

            // 获取操作者角色
            ProjectMemberRole operatorRole = getUserRole(projectId, operatorId);
            // 管理员不能移除其他管理员
            if (operatorRole == ProjectMemberRole.ADMIN && member.getProjectRole() == ProjectMemberRole.ADMIN) {
                return R.fail("管理员不能移除其他管理员，只有项目负责人可以");
            }

            // 在删除前获取所有成员ID（包括即将被移除的成员）
            List<Long> allMemberIds = getProjectMemberUserIds(projectId);
            
            // 删除成员
            projectMemberRepository.delete(member);
            
            // 向所有项目成员发送成员移除消息
            try {
                if (project != null && !allMemberIds.isEmpty()) {
                    inboxMessageService.sendBatchPersonalMessage(
                            MessageScene.PROJECT_MEMBER_REMOVED,
                            operatorId,
                            allMemberIds,
                            "成员离开项目",
                            String.format("有成员已离开项目「%s」", project.getName()),
                            projectId,
                            "PROJECT",
                            null
                    );
                }
            } catch (Exception e) {
                log.warn("发送项目成员移除消息失败: projectId={}, userId={}", projectId, userId, e);
            }

            log.info("移除项目成员成功: projectId={}, operatorId={}, userId={}", projectId, operatorId, userId);
            return R.ok();
        } catch (Exception e) {
            log.error("移除项目成员失败: projectId={}, operatorId={}, userId={}", projectId, operatorId, userId, e);
            return R.fail("移除成员失败: " + e.getMessage());
        }
    }

    /**
     * 更新成员角色
     *
     * @param projectId 项目ID
     * @param userId 用户ID
     * @param newRole 新角色
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> updateMemberRole(Long projectId, Long userId, ProjectMemberRole newRole) {
        try {
            // 1. 查询成员
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElse(null);
            if (member == null) {
                return R.fail("该用户不是项目成员");
            }

            // 简化规则：仅禁止将已有 OWNER 修改为其他角色
            if (member.getProjectRole() == ProjectMemberRole.OWNER && newRole != ProjectMemberRole.OWNER) {
                return R.fail("不能修改项目拥有者的角色");
            }

            ProjectMemberRole oldRole = member.getProjectRole();
            member.setProjectRole(newRole);
            projectMemberRepository.save(member);
            
            // 发送项目角色变更消息
            try {
                Project project = projectRepository.findById(projectId).orElse(null);
                if (project != null && oldRole != newRole) {
                    inboxMessageService.sendPersonalMessage(
                            MessageScene.PROJECT_ROLE_CHANGED,
                            null, // 系统消息
                            userId,
                            "项目角色变更",
                            String.format("您在项目「%s」中的角色已从「%s」变更为「%s」", 
                                    project.getName(), oldRole.getDescription(), newRole.getDescription()),
                            projectId,
                            "PROJECT",
                            null
                    );
                }
            } catch (Exception e) {
                log.warn("发送项目角色变更消息失败: projectId={}, userId={}", projectId, userId, e);
            }

            log.info("更新项目成员角色成功: projectId={}, userId={}, newRole={}", projectId, userId, newRole);
            return R.ok();
        } catch (Exception e) {
            log.error("更新成员角色失败: projectId={}, userId={}, newRole={}", projectId, userId, newRole, e);
            return R.fail("更新角色失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Void> updateMemberRole(Long projectId, Long userId, ProjectMemberRole newRole, Long operatorId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

            if (!isAdmin(projectId, operatorId)) {
                return R.fail("只有项目管理员可以修改成员角色");
            }

            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElse(null);
            if (member == null) {
                return R.fail("该用户不是项目成员");
            }

            if (member.getProjectRole() == ProjectMemberRole.OWNER) {
                return R.fail("不能修改项目负责人的角色");
            }

            ProjectMemberRole operatorRole = getUserRole(projectId, operatorId);
            if (operatorRole == ProjectMemberRole.ADMIN && member.getProjectRole() == ProjectMemberRole.ADMIN) {
                return R.fail("管理员不能修改其他管理员的角色，只有项目负责人可以");
            }

            if (operatorRole == ProjectMemberRole.ADMIN && newRole == ProjectMemberRole.OWNER) {
                return R.fail("管理员不能将成员设置为项目负责人");
            }

            ProjectMemberRole oldRole = member.getProjectRole();
            member.setProjectRole(newRole);
            projectMemberRepository.save(member);
            
            // 发送项目角色变更消息
            try {
                if (oldRole != newRole) {
                    inboxMessageService.sendPersonalMessage(
                            MessageScene.PROJECT_ROLE_CHANGED,
                            operatorId,
                            userId,
                            "项目角色变更",
                            String.format("您在项目「%s」中的角色已从「%s」变更为「%s」", 
                                    project.getName(), oldRole.getDescription(), newRole.getDescription()),
                            projectId,
                            "PROJECT",
                            null
                    );
                }
            } catch (Exception e) {
                log.warn("发送项目角色变更消息失败: projectId={}, userId={}", projectId, userId, e);
            }

            log.info("更新项目成员角色成功: projectId={}, operatorId={}, userId={}, newRole={}", projectId, operatorId, userId, newRole);
            return R.ok();
        } catch (Exception e) {
            log.error("更新项目成员角色失败: projectId={}, operatorId={}, userId={}, newRole={}", projectId, operatorId, userId, newRole, e);
            return R.fail("更新成员角色失败: " + e.getMessage());
        }
    }

    @Override
    public Page<ProjectMember> getMyProjects(Long userId, Pageable pageable) {
        return projectMemberRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<ProjectMember> getProjectMembers(Long projectId, Pageable pageable) {
        return projectMemberRepository.findByProjectId(projectId, pageable);
    }

    @Override
    public List<ProjectMember> getMembersByRole(Long projectId, ProjectMemberRole role) {
        return projectMemberRepository.findByProjectIdAndProjectRole(projectId, role);
    }

    @Override
    public boolean isMember(Long projectId, Long userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public boolean isOwner(Long projectId, Long userId) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        return memberOpt.map(m -> m.getProjectRole() == ProjectMemberRole.OWNER).orElse(false);
    }

    @Override
    public boolean isAdmin(Long projectId, Long userId) {
        Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
        return memberOpt.map(m -> {
            ProjectMemberRole role = m.getProjectRole();
            return role == ProjectMemberRole.OWNER || role == ProjectMemberRole.ADMIN;
        }).orElse(false);
    }

    @Override
    public ProjectMemberRole getUserRole(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getProjectRole)
                .orElse(null);
    }

    @Override
    public long getMemberCount(Long projectId) {
        return projectMemberRepository.countByProjectId(projectId);
    }

    @Override
    public List<Long> getProjectMemberUserIds(Long projectId) {
        return projectMemberRepository.findUserIdsByProjectId(projectId);
    }
    
    /**
     * 获取项目成员详细信息列表（包含用户名称）
     * @param projectId 项目ID
     * @param pageable 分页参数
     * @return 成员详细信息分页列表
     */
    public Page<ProjectMemberDetailDTO> getProjectMembersWithDetails(Long projectId, Pageable pageable) {
        Page<ProjectMember> memberPage = projectMemberRepository.findByProjectId(projectId, pageable);
        List<ProjectMemberDetailDTO> detailList = memberPage.getContent().stream()
                .map(this::convertToDetailDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(detailList, pageable, memberPage.getTotalElements());
    }
    
    /**
     * 将ProjectMember转换为ProjectMemberDetailDTO
     * @param member 项目成员实体
     * @return 成员详细信息DTO
     */
    private ProjectMemberDetailDTO convertToDetailDTO(ProjectMember member) {
        if (member == null) {
            return null;
        }
        
        // 查询用户信息
        String username = "未知用户";
        String email = "";
        if (member.getUserId() != null) {
            try {
                R<UserDTO> userResult = userService.getCurrentUser(member.getUserId());
                if (R.isSuccess(userResult) && userResult.getData() != null) {
                    UserDTO user = userResult.getData();
                    username = user.getName();
                    email = user.getEmail();
                }
            } catch (Exception e) {
                log.warn("查询用户信息失败: userId={}", member.getUserId(), e);
            }
        }
        
        // 查询项目名称
        String projectName = "";
        if (member.getProjectId() != null) {
            try {
                Project project = projectRepository.findById(member.getProjectId()).orElse(null);
                if (project != null) {
                    projectName = project.getName();
                }
            } catch (Exception e) {
                log.warn("查询项目名称失败: projectId={}", member.getProjectId(), e);
            }
        }
        
        return ProjectMemberDetailDTO.builder()
                .id(member.getId())
                .projectId(member.getProjectId())
                .projectName(projectName)
                .userId(member.getUserId())
                .username(username)
                .email(email)
                .projectRole(member.getProjectRole())
                .roleName(member.getProjectRole() != null ? member.getProjectRole().getDescription() : "")
                .joinedAt(member.getJoinedAt())
                .isCurrentUser(false)
                .build();
    }
}

