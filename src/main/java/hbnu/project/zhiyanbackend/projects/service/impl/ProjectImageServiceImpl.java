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
 * 项目图片服务接口实现类
 * 负责处理项目封面图片在数据库中的存储与读取
 * 实现了ProjectImageService接口中定义的方法
 *
 * @author Tokito
 */

@Slf4j  // 使用Lombok的日志注解，用于日志记录
@Service  // 标识该类为Spring服务组件
@RequiredArgsConstructor  // 使用Lombok的构造函数注解，自动生成带final字段的构造函数
public class ProjectImageServiceImpl implements ProjectImageService {  // 实现项目图片服务接口

    private final ProjectRepository projectRepository;  // 项目数据访问层，用于数据库操作



    /**
     * 更新项目封面图片
     * 将上传的图片文件保存到数据库中，并更新图片访问URL
     *
     * @param projectId 项目ID，用于标识需要更新的项目
     * @param file      上传的图片文件，包含图片的二进制数据
     * @return 返回操作结果，成功返回R.ok()，失败返回R.fail()并附带错误信息
     */
    @Override  // 标识为重写父类方法
    @Transactional  // 开启事务，确保方法执行的数据一致性
    public R<Void> updateProjectImage(Long projectId, MultipartFile file) {
        try {
            // 根据项目ID查询项目信息，如果不存在则返回失败
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            // 检查上传的文件是否为空
            if (file == null || file.isEmpty()) {
                return R.fail("上传文件为空");
            }

            // 仅将图片二进制数据保存在 PostgreSQL 的 image_data(bytea) 字段中
            byte[] imageData = file.getBytes();
            project.setImageData(imageData);
            // 同时更新可直接访问的图片URL，便于前端在刷新后仍能展示
            project.setImageUrl("/zhiyan/projects/get-image?projectId=" + projectId);

            // 保存更新后的项目信息到数据库
            projectRepository.save(project);

            // 记录成功日志，包含项目ID和图片大小
            log.info("更新项目封面图片成功: projectId={}, size={}",
                    projectId,
                    imageData.length);
            return R.ok();
        } catch (Exception e) {
            // 记录失败日志，包含项目ID和异常信息
            log.error("更新项目封面图片失败: projectId={}", projectId, e);
            return R.fail("更新项目图片失败: " + e.getMessage());
        }
    }



    /**
     * 获取项目封面图片
     * 根据项目ID从数据库中获取项目的图片二进制数据
     *
     * @param projectId 项目ID，用于标识需要获取图片的项目
     * @return 返回操作结果，成功返回R.ok()并包含图片数据，失败返回R.fail()并附带错误信息
     */
    @Override  // 标识为重写父类方法
    @Transactional(readOnly = true)  // 开启只读事务，优化查询性能
    public R<byte[]> getProjectImage(Long projectId) {
        try {
            // 根据项目ID查询项目信息，如果不存在则返回失败
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                return R.fail("项目不存在");
            }

            // 目前优先返回数据库中的二进制数据
            return R.ok(project.getImageData());
        } catch (Exception e) {
            // 记录失败日志，包含项目ID和异常信息
            log.error("获取项目封面图片失败: projectId={}", projectId, e);
            return R.fail("获取项目图片失败: " + e.getMessage());
        }
    }
}

