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
 */
public interface ProjectService {

    R<Project> createProject(String name,
                             String description,
                             ProjectVisibility visibility,
                             LocalDate startDate,
                             LocalDate endDate,
                             String imageUrl,
                             Long creatorId);

    R<Project> updateProject(Long projectId,
                             String name,
                             String description,
                             ProjectVisibility visibility,
                             ProjectStatus status,
                             LocalDate startDate,
                             LocalDate endDate,
                             String imageUrl);

    R<Void> deleteProject(Long projectId, Long userId);

    R<Project> getProjectById(Long projectId);

    R<Page<Project>> getAllProjects(Pageable pageable);

    R<Page<Project>> getProjectsByCreator(Long creatorId, Pageable pageable);

    R<Page<Project>> getProjectsByStatus(ProjectStatus status, Pageable pageable);

    R<Page<Project>> getUserProjects(Long userId, Pageable pageable);

    R<Page<Project>> searchProjects(String keyword, Pageable pageable);

    R<Page<Project>> getPublicActiveProjects(Pageable pageable);

    R<Project> updateProjectStatus(Long projectId, ProjectStatus status);

    R<Void> archiveProject(Long projectId, Long userId);

    R<Boolean> hasAccessPermission(Long projectId, Long userId);

    R<Long> countUserCreatedProjects(Long userId);

    R<Long> countUserParticipatedProjects(Long userId);
}

