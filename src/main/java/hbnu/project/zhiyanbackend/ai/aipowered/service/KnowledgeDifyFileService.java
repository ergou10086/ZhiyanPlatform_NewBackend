package hbnu.project.zhiyanbackend.ai.aipowered.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * KnowledgeDifyFileService 接口，提供与Dify文件相关的服务功能
 * 该接口定义了文件上传和知识文件上传的方法
 */
public interface KnowledgeDifyFileService {

    /**
     * 上传单个文件到Dify系统
     * @param file 要上传的文件，使用MultipartFile类型接收
     * @param userId 用户ID，用于标识文件所属用户
     * @return 返回上传结果，包含文件在Dify系统中的相关信息
     */
    DifyFileUploadResponse uploadFile(MultipartFile file, Long userId);

    /**
     * 批量上传文件到Dify系统
     * @param files 要上传的文件列表，使用List<MultipartFile>类型接收
     * @param userId 用户ID，用于标识文件所属用户
     * @return 返回上传结果列表，每个元素对应一个文件的上传结果
     */
    List<DifyFileUploadResponse> uploadFiles(List<MultipartFile> files, Long userId);

    /**
     * 上传知识文件到Dify系统
     * @param fileIds 已上传文件的ID列表，这些文件将被作为知识文件上传
     * @param userId 用户ID，用于标识知识文件所属用户
     * @return 返回上传结果列表，包含知识文件在Dify系统中的相关信息
     */
    List<DifyFileUploadResponse> uploadKnowledgeFiles(List<Long> fileIds, Long userId);
}
