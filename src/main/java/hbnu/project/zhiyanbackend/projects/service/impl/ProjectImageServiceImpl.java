package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectImageServiceImpl implements ProjectImageService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public R<Void> updateProjectImage(Long projectId, byte[] imageData, String contentType) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            project.setImageData(imageData);
            projectRepository.save(project);

            log.info("更新项目封面图片成功: projectId={}, size={}", projectId,
                    imageData != null ? imageData.length : 0);
            return R.ok();
        } catch (Exception e) {
            log.error("更新项目封面图片失败: projectId={}", projectId, e);
            return R.fail("更新项目图片失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public R<byte[]> getProjectImage(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            return R.ok(project.getImageData());
        } catch (Exception e) {
            log.error("获取项目封面图片失败: projectId={}", projectId, e);
            return R.fail("获取项目图片失败: " + e.getMessage());
        }
    }
}

