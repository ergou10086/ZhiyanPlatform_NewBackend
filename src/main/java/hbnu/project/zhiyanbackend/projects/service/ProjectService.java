package hbnu.project.zhiyanbackend.projects.service;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectStatus;
import hbnu.project.zhiyanbackend.projects.model.enums.ProjectVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * 项目服务接口（精简版）
 * 定义了项目相关的核心业务操作，包括项目的创建、更新、删除、查询等功能
 *
 * @author Tokito
 */
public interface ProjectService {

    /**
     * 创建新项目
     * @param name 项目名称
     * @param description 项目描述
     * @param visibility 项目可见性
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param imageUrl 项目图片URL
     * @param creatorId 创建者ID
     * @return 返回创建结果，包含项目信息
     */
    R<Project> createProject(String name,
                             String description,
                             ProjectVisibility visibility,
                             LocalDate startDate,
                             LocalDate endDate,
                             String imageUrl,
                             Long creatorId);

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
     * @return 返回更新结果，包含更新后的项目信息
     */
    R<Project> updateProject(Long projectId,
                             String name,
                             String description,
                             ProjectVisibility visibility,
                             ProjectStatus status,
                             LocalDate startDate,
                             LocalDate endDate,
                             String imageUrl);

    /**
     * 删除项目
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 返回操作结果
     */
    R<Void> deleteProject(Long projectId, Long userId);

    /**
     * 根据ID获取项目
     * @param projectId 项目ID
     * @return 返回项目信息
     */
    R<Project> getProjectById(Long projectId);

    /**
     * 获取所有项目（分页）
     * @param pageable 分页参数
     * @return 返回分页后的项目列表
     */
    R<Page<Project>> getAllProjects(Pageable pageable);

    /**
     * 获取指定创建者的所有项目（分页）
     * @param creatorId 创建者ID
     * @param pageable 分页参数
     * @return 返回分页后的项目列表
     */
    R<Page<Project>> getProjectsByCreator(Long creatorId, Pageable pageable);

    /**
     * 获取指定状态的所有项目（分页）
     * @param status 项目状态
     * @param pageable 分页参数
     * @return 返回分页后的项目列表
     */
    R<Page<Project>> getProjectsByStatus(ProjectStatus status, Pageable pageable);

    /**
     * 获取用户参与的所有项目（分页）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 返回分页后的项目列表
     */
    R<Page<Project>> getUserProjects(Long userId, Pageable pageable);

    /**
     * 搜索项目（分页）
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 返回分页后的搜索结果
     */
    R<Page<Project>> searchProjects(String keyword, Pageable pageable);

    /**
     * 获取所有公开的活跃项目（分页）
     * @param pageable 分页参数
     * @return 返回分页后的项目列表
     */
    R<Page<Project>> getPublicActiveProjects(Pageable pageable);

    /**
     * 更新项目状态
     * @param projectId 项目ID
     * @param status 新的项目状态
     * @return 返回更新结果，包含更新后的项目信息
     */
    R<Project> updateProjectStatus(Long projectId, ProjectStatus status);

    /**
     * 归档项目
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 返回操作结果
     */
    R<Void> archiveProject(Long projectId, Long userId);

    /**
     * 检查用户对项目的访问权限
     * @param projectId 项目ID
     * @param userId 用户ID
     * @return 返回是否有访问权限
     */
    R<Boolean> hasAccessPermission(Long projectId, Long userId);

    /**
     * 统计用户创建的项目数量
     * @param userId 用户ID
     * @return 返回项目数量
     */
    R<Long> countUserCreatedProjects(Long userId);

    /**
     * 统计用户参与的项目数量
     * @param userId 用户ID
     * @return 返回项目数量
     */
    R<Long> countUserParticipatedProjects(Long userId);

    /**
     * 保存项目草稿
     * @param name 项目名称
     * @param description 项目描述
     * @param visibility 项目可见性
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param imageUrl 项目图片URL
     * @param creatorId 创建者ID
     * @return 返回保存结果，包含草稿项目信息
     */
    R<Project> saveDraft(String name,String description,
                         ProjectVisibility visibility,LocalDate startDate,
                         LocalDate endDate,String imageUrl,Long creatorId);

    /**
     * 获取用户的草稿项目
     * @param userId 用户ID
     * @return 返回草稿项目信息，如果不存在则返回空
     */
    R<Project> getDraft(Long userId);

    /**
     * 删除用户的草稿项目
     * @param userId 用户ID
     * @return 返回操作结果
     */
    R<Void> deleteDraft(Long userId);
}

