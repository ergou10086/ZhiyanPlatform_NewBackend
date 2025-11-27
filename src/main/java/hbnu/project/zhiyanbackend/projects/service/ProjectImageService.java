package hbnu.project.zhiyanbackend.projects.service;

import hbnu.project.zhiyanbackend.basic.domain.R;
import org.springframework.web.multipart.MultipartFile;

/**
 * 项目图片服务接口
 * 负责处理项目封面图片在数据库中的存储与读取
 */
public interface ProjectImageService {

    /**
     * 更新指定项目的封面图片
     *
     * @param projectId 项目ID
     * @param file 图片文件
     * @return 结果
     */
    R<Void> updateProjectImage(Long projectId, MultipartFile file);

    /**
     * 获取指定项目的封面图片二进制数据
     *
     * @param projectId 项目ID
     * @return 图片二进制数据（可能为 null）
     */
    R<byte[]> getProjectImage(Long projectId);
}

