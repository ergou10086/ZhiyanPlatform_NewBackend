package hbnu.project.zhiyanbackend.projects.service.impl;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.oss.dto.UploadFileResponseDTO;
import hbnu.project.zhiyanbackend.oss.service.COSService;
import hbnu.project.zhiyanbackend.projects.model.entity.Project;
import hbnu.project.zhiyanbackend.projects.repository.ProjectRepository;
import hbnu.project.zhiyanbackend.projects.service.ProjectImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectImageServiceImpl implements ProjectImageService {

    private final ProjectRepository projectRepository;
    private final COSService cosService;

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

            // 1. 上传到 COS 对象存储，使用业务目录 project_cover
            UploadFileResponseDTO uploadResult = cosService.uploadFileSenior(file, "project_cover", null);

            // 2. 可选：同时在数据库中保留一份二进制数据，兼容现有 GET /image 接口
            byte[] imageData = file.getBytes();
            project.setImageData(imageData);

            // 3. 记录对象存储信息，便于前端或后续服务直接访问 COS 资源
            project.setImageObjectKey(uploadResult.getObjectKey());
            project.setImageUrl(uploadResult.getUrl());

            projectRepository.save(project);

            log.info("更新项目封面图片成功: projectId={}, size={}, objectKey={}",
                    projectId,
                    imageData.length,
                    uploadResult.getObjectKey());
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

