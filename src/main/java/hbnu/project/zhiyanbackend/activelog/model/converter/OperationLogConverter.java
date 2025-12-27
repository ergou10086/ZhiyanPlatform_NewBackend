package hbnu.project.zhiyanbackend.activelog.model.converter;


import hbnu.project.zhiyanbackend.activelog.model.dto.ProjectLogExportDTO;
import hbnu.project.zhiyanbackend.activelog.model.dto.UnifiedLogExportDTO;
import hbnu.project.zhiyanbackend.activelog.model.entity.AchievementOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.ProjectOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.TaskOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.entity.WikiOperationLog;
import hbnu.project.zhiyanbackend.activelog.model.vo.UnifiedOperationLogVO;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 操作日志转换类
 *
 * @author yui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogConverter {

    private final OperationLogMapper operationLogMapper;
    private final UserRepository userRepository;

    public UnifiedOperationLogVO toUnifiedVO(ProjectOperationLog log) {
        UnifiedOperationLogVO vo = operationLogMapper.mapProject(log);
        fillRealUsername(vo);
        return vo;
    }

    public UnifiedOperationLogVO toUnifiedVO(TaskOperationLog log) {
        UnifiedOperationLogVO vo = operationLogMapper.mapTask(log);
        fillRealUsername(vo);
        return vo;
    }

    public UnifiedOperationLogVO toUnifiedVO(WikiOperationLog log) {
        UnifiedOperationLogVO vo = operationLogMapper.mapWiki(log);
        fillRealUsername(vo);
        return vo;
    }

    public UnifiedOperationLogVO toUnifiedVO(AchievementOperationLog log) {
        UnifiedOperationLogVO vo = operationLogMapper.mapAchievement(log);
        fillRealUsername(vo);
        return vo;
    }
    
    /**
     * 填充真实用户名（如果username是邮箱格式，则通过userId获取真实用户名）
     */
    private void fillRealUsername(UnifiedOperationLogVO vo) {
        if (vo == null || vo.getUserId() == null) {
            return;
        }
        
        // 如果username是邮箱格式，则通过userId获取真实用户名
        String currentUsername = vo.getUsername();
        if (currentUsername != null && currentUsername.contains("@")) {
            try {
                String realName = userRepository.findNameById(vo.getUserId())
                        .orElse(currentUsername);
                vo.setUsername(realName);
            } catch (Exception e) {
                log.warn("获取用户真实姓名失败: userId={}, error={}", vo.getUserId(), e.getMessage());
                // 如果获取失败，保持原值
            }
        }
    }

    public List<UnifiedOperationLogVO> toUnifiedVOList(List<?> logs) {
        return logs.stream().map(log -> {
            if (log instanceof ProjectOperationLog) {
                return toUnifiedVO((ProjectOperationLog) log);
            } else if (log instanceof TaskOperationLog) {
                return toUnifiedVO((TaskOperationLog) log);
            } else if (log instanceof WikiOperationLog) {
                return toUnifiedVO((WikiOperationLog) log);
            } else if (log instanceof AchievementOperationLog) {
                return toUnifiedVO((AchievementOperationLog) log);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 将项目操作日志转换为导出DTO
     */
    public ProjectLogExportDTO toProjectExportDTO(ProjectOperationLog log) {
        return operationLogMapper.toProjectExportDTO(log);
    }

    /**
     * 将统一日志VO转换为导出DTO
     */
    public UnifiedLogExportDTO toUnifiedExportDTO(UnifiedOperationLogVO vo) {
        return operationLogMapper.toUnifiedExportDTO(vo);
    }

    /**
     * 批量将项目操作日志转换为导出DTO列表
     */
    public List<ProjectLogExportDTO> toProjectExportDTOList(List<ProjectOperationLog> logs) {
        return logs.stream()
                .map(this::toProjectExportDTO)
                .collect(Collectors.toList());
    }

    /**
     * 批量将统一日志VO转换为导出DTO列表
     */
    public List<UnifiedLogExportDTO> toUnifiedExportDTOList(List<UnifiedOperationLogVO> vos) {
        return vos.stream()
                .map(this::toUnifiedExportDTO)
                .collect(Collectors.toList());
    }
}