package hbnu.project.zhiyanbackend.projects.service;

import hbnu.project.zhiyanbackend.basic.domain.R;

/**
 * 项目图片服务接口
 * 负责处理项目封面图片在数据库中的存储与读取
 */
public interface ProjectImageService {

    /**
     * 更新指定项目的封面图片
     *
     * @param projectId 项目ID
     * @param imageData 图片二进制数据
     * @param contentType 图片 MIME 类型（可选）
     * @return 结果
     */
    R<Void> updateProjectImage(Long projectId, byte[] imageData, String contentType);

    /**
     * 获取指定项目的封面图片二进制数据
     *
     * @param projectId 项目ID
     * @return 图片二进制数据（可能为 null）
     */
    R<byte[]> getProjectImage(Long projectId);
}

