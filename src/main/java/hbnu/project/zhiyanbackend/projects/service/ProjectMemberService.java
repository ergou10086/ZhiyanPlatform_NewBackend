package hbnu.project.zhiyanbackend.projects.service;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.entity.ProjectMember;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectMemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 项目成员服务接口（精简版）
 */
public interface ProjectMemberService {

    /**
     * 内部添加成员（无外部校验），用于创建项目时自动添加创建者等场景
     */
    ProjectMember addMemberInternal(Long projectId, Long userId, ProjectMemberRole role);

    /**
     * 通过接口添加成员（不依赖外部用户服务，只做去重和项目存在性校验）
     */
    R<Void> addMember(Long projectId, Long userId, ProjectMemberRole role);

    /**
     * 移除项目成员
     */
    R<Void> removeMember(Long projectId, Long userId);

    /**
     * 更新成员角色
     */
    R<Void> updateMemberRole(Long projectId, Long userId, ProjectMemberRole newRole);

    R<Void> removeMember(Long projectId, Long userId, Long operatorId);

    R<Void> updateMemberRole(Long projectId, Long userId, ProjectMemberRole newRole, Long operatorId);

    /**
     * 获取我参与的项目成员记录分页
     */
    Page<ProjectMember> getMyProjects(Long userId, Pageable pageable);

    /**
     * 获取某个项目的成员分页列表
     */
    Page<ProjectMember> getProjectMembers(Long projectId, Pageable pageable);

    /**
     * 获取项目中指定角色的成员
     */
    List<ProjectMember> getMembersByRole(Long projectId, ProjectMemberRole role);

    boolean isMember(Long projectId, Long userId);

    boolean isOwner(Long projectId, Long userId);

    boolean isAdmin(Long projectId, Long userId);

    ProjectMemberRole getUserRole(Long projectId, Long userId);

    long getMemberCount(Long projectId);

    List<Long> getProjectMemberUserIds(Long projectId);
}

