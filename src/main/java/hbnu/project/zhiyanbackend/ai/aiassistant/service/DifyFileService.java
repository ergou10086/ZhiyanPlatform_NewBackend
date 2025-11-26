package hbnu.project.zhiyanbackend.ai.aiassistant.service;

import hbnu.project.zhiyanbackend.ai.aiassistant.model.response.DifyFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DifyFileService {

    DifyFileUploadResponse uploadFile(MultipartFile file, Long userId);

    List<DifyFileUploadResponse> uploadFiles(List<MultipartFile> files, Long userId);

    List<DifyFileUploadResponse> uploadKnowledgeFiles(List<Long> fileIds, Long userId);
}
