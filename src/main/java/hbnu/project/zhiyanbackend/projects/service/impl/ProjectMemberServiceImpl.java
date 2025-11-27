package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import hbnu.project.zhiyanbackend.projects.repository.ProjectMemberRepository;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectMemberService;
import hbnu.project.zhiyanbackend.message.service.InboxMessageService;
import hbnu.project.zhiyanbackend.message.model.enums.MessageScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 项目成员服务实现（精简版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final InboxMessageService inboxMessageService;

    @Override
    @Transactional
    public ProjectMember addMemberInternal(Long projectId, Long userId, ProjectMemberRole role) {
        // TODO 后续接入认证模块时，可在此校验 userId 是否为有效用户

        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("项目不存在");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("用户[{}]已经是项目[{}]成员", userId, projectId);
            return projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        }

        ProjectMember member = ProjectMember.builder()
                .projectId(projectId)
                .userId(userId)
                .projectRole(role)
                .joinedAt(LocalDateTime.now())
                .build();

        ProjectMember saved = projectMemberRepository.save(member);
        log.info("内部添加项目成员成功: projectId={}, userId={}, role={}", projectId, userId, role);
        return saved;
    }

    @Override
    @Transactional
    public R<Void> addMember(Long projectId, Long userId, ProjectMemberRole role) {
        try {
            // TODO 后续接入认证模块时，可在此校验 userId 是否存在 / 是否允许被邀请

            if (!projectRepository.existsById(projectId)) {
                return R.fail("项目不存在");
            }

            if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
                return R.fail("该用户已经是项目成员");
            }

            ProjectMember member = ProjectMember.builder()
                    .projectId(projectId)
                    .userId(userId)
                    .projectRole(role)
                    .joinedAt(LocalDateTime.now())
                    .build();

            projectMemberRepository.save(member);
            
            // 向所有项目成员发送成员加入消息
            try {
                Project project = projectRepository.findById(projectId).orElse(null);
                if (project != null) {
                    List<Long> allMemberIds = getProjectMemberUserIds(projectId);
                    if (!allMemberIds.isEmpty()) {
                        inboxMessageService.sendBatchPersonalMessage(
                                MessageScene.PROJECT_MEMBER_INVITED,
                                null, // 系统消息
                                allMemberIds,
                                "新成员加入项目",
                                String.format("新成员已加入项目「%s」，角色：%s", project.getName(), role.getDescription()),
                                projectId,
                                "PROJECT",
                                null
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("发送项目成员加入消息失败: projectId={}, userId={}", projectId, userId, e);
            }

            log.info("添加项目成员成功: projectId={}, userId={}, role={}", projectId, userId, role);
            return R.ok();
        } catch (Exception e) {
            log.error("添加项目成员失败: projectId={}, userId={}, role={}", projectId, userId, role, e);
            return R.fail("添加成员失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public R<Void> removeMember(Long projectId, Long userId) {
        try {
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElse(null);
            if (member == null) {
                return R.fail("该用户不是项目成员");
            }

            // TODO 后续接入认证/权限模块时，可在此增加删除成员的权限校验（如仅 OWNER/ADMIN 可移除他人）

            // 在删除前获取所有成员ID（包括即将被移除的成员）
            List<Long> allMemberIds = getProjectMemberUserIds(projectId);
            
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

    @Override
    @Transactional
    public R<Void> removeMember(Long projectId, Long userId, Long operatorId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

            if (!isAdmin(projectId, operatorId)) {
                return R.fail("只有项目管理员可以移除成员");
            }

            if (userId.equals(operatorId)) {
                return R.fail("不能移除自己，如需退出项目请使用退出功能");
            }

            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElse(null);
            if (member == null) {
                return R.fail("该用户不是项目成员");
            }

            if (member.getProjectRole() == ProjectMemberRole.OWNER) {
                return R.fail("不能移除项目负责人");
            }

            ProjectMemberRole operatorRole = getUserRole(projectId, operatorId);
            if (operatorRole == ProjectMemberRole.ADMIN && member.getProjectRole() == ProjectMemberRole.ADMIN) {
                return R.fail("管理员不能移除其他管理员，只有项目负责人可以");
            }

            // 在删除前获取所有成员ID（包括即将被移除的成员）
            List<Long> allMemberIds = getProjectMemberUserIds(projectId);
            
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

            // TODO 后续接入认证/权限模块时，可在此扩展更细粒度的角色变更规则

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
}

