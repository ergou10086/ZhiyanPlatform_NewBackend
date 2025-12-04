package hbnu.project.zhiyanbackend.ai.aiassistant.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Dify文件服务接口
 * 该接口定义了文件上传和知识文件上传的相关操作方法
 *
 * @author Tokito
 */
public interface DifyFileService {

    /**
     * 上传单个文件
     * @param file 要上传的文件，类型为MultipartFile
     * @param userId 用户ID，用于标识文件所属用户
     * @return 返回上传结果，包含文件相关信息
     */
    DifyFileUploadResponse uploadFile(MultipartFile file, Long userId);

    /**
     * 批量上传文件
     * @param files 要上传的文件列表，类型为List<MultipartFile>
     * @param userId 用户ID，用于标识文件所属用户
     * @return 返回上传结果列表，每个元素包含对应文件的相关信息
     */
    List<DifyFileUploadResponse> uploadFiles(List<MultipartFile> files, Long userId);

    /**
     * 上传知识文件
     * @param fileIds 文件ID列表，用于标识要上传的知识文件
     * @param userId 用户ID，用于标识文件所属用户
     * @return 返回上传结果列表，每个元素包含对应文件的相关信息
     */
    List<DifyFileUploadResponse> uploadKnowledgeFiles(List<Long> fileIds, Long userId);
}
