package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 项目图片服务接口
 * 负责处理项目封面图片在数据库中的存储与读取
 *
 * @author Tokito
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectImageServiceImpl implements ProjectImageService {

    private final ProjectRepository projectRepository;

    @Override
    @Transactional
    public R<Void> updateProjectImage(Long projectId, MultipartFile file) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            if (file == null || file.isEmpty()) {
                return R.fail("上传文件为空");
            }

            // 仅将图片二进制数据保存在 PostgreSQL 的 image_data(bytea) 字段中
            byte[] imageData = file.getBytes();
            project.setImageData(imageData);
            // 同时更新可直接访问的图片URL，便于前端在刷新后仍能展示
            project.setImageUrl("/zhiyan/projects/get-image?projectId=" + projectId);

            projectRepository.save(project);

            log.info("更新项目封面图片成功: projectId={}, size={}",
                    projectId,
                    imageData.length);
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

            // 目前优先返回数据库中的二进制数据
            return R.ok(project.getImageData());
        } catch (Exception e) {
            log.error("获取项目封面图片失败: projectId={}", projectId, e);
            return R.fail("获取项目图片失败: " + e.getMessage());
        }
    }
}

