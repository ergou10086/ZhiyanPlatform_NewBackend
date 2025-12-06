package hbnu.project.zhiyanbackend.tasks.service;

import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionFileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 任务提交附件存储服务
 * 该接口定义了任务提交附件的存储和删除操作
 */
public interface TaskSubmissionFileService {

    /**
     * 存储单个文件
     * @param file 要存储的文件
     * @return TaskSubmissionFileResponse 包含存储结果的响应对象
     */
    TaskSubmissionFileResponse store(MultipartFile file);

    /**
     * 批量存储多个文件
     * @param files 要存储的文件列表
     * @return List<TaskSubmissionFileResponse> 包含所有存储结果的响应对象列表
     */
    List<TaskSubmissionFileResponse> storeBatch(List<MultipartFile> files);

    /**
     * 删除指定相对路径的文件
     * @param relativePath 文件的相对路径
     * @return boolean 删除操作是否成功
     */
    boolean delete(String relativePath);

    /**
     * 批量删除多个文件
     * @param relativePaths 要删除的文件相对路径列表
     * @return int 成功删除的文件数量
     */
    int deleteBatch(List<String> relativePaths);
}


