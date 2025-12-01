package hbnu.project.zhiyanbackend.tasks.service;

import hbnu.project.zhiyanbackend.tasks.model.dto.TaskSubmissionFileResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 任务提交附件存储服务
 */
public interface TaskSubmissionFileService {

    TaskSubmissionFileResponse store(MultipartFile file);

    List<TaskSubmissionFileResponse> storeBatch(List<MultipartFile> files);

    boolean delete(String relativePath);

    int deleteBatch(List<String> relativePaths);

    Resource loadAsResource(String relativePath);
}


