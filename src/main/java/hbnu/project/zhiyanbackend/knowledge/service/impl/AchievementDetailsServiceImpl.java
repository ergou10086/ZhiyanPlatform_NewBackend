package hbnu.project.zhiyanbackend.knowledge.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.basic.utils.SnowflakeIdUtils;
import hbnu.project.zhiyanbackend.knowledge.model.converter.AchievementFileConverter;
import hbnu.project.zhiyanbackend.knowledge.model.dto.*;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementDetail;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementStatus;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementType;
import hbnu.project.zhiyanbackend.knowledge.model.template.AchievementTemplate;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementDetailRepository;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementDetailsService;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementFileService;
import hbnu.project.zhiyanbackend.message.service.MessageSendService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成果详情服务实现
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementDetailsServiceImpl implements AchievementDetailsService {
    @Resource
    private AchievementRepository achievementRepository;

    @Resource
    private AchievementDetailRepository achievementDetailRepository;

    @Resource
    private AchievementFileService achievementFileService;

    @Resource
    private MessageSendService messageSendService;

    @Resource
    private JsonUtils jsonUtils;

    private final AchievementFileConverter achievementFileConverter;

    /**
     * 创建成果及其详情
     * 一次性创建成果主记录和详情记录
     *
     * @param createDTO 创建DTO
     * @return 成果信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AchievementDTO createAchievementWithDetails(CreateAchievementDTO createDTO) {
        log.info("开始创建成果及其详情: projectId={}, title={}, type={}",
                createDTO.getProjectId(), createDTO.getTitle(), createDTO.getType());

        // 1. 创建成果主记录
        Achievement achievement = Achievement.builder()
                .id(SnowflakeIdUtils.nextId())
                .projectId(createDTO.getProjectId())
                .title(createDTO.getTitle())
                .type(createDTO.getType())
                .creatorId(createDTO.getCreatorId())
                .status(createDTO.getStatus() != null ? createDTO.getStatus() : AchievementStatus.draft)
                .isPublic(createDTO.getIsPublic() != null ? createDTO.getIsPublic() : false)
                .build();

        // 2. 持久化成果主记录
        achievement = achievementRepository.save(achievement);
        log.info("成果主记录创建成功: achievementId={}", achievement.getId());

        // 3. 创建成果详情记录
        String detailDataJson = jsonUtils.convertToJson(createDTO.getDetailData());

        AchievementDetail detail = AchievementDetail.builder()
                .id(SnowflakeIdUtils.nextId())
                .achievementId(achievement.getId())
                .detailData(detailDataJson)
                .abstractText(createDTO.getAbstractText())
                .build();

        achievementDetailRepository.save(detail);
        log.info("成果详情记录创建成功: achievementId={}", achievement.getId());

        // 4.发送成果的创建通知
        messageSendService.notifyAchievementCreated(achievement);

        // 5. 转换为DTO返回
        return buildAchievementDTO(achievement);
    }

    /**
     * 获取成果详情
     * 包含主表、详情、文件列表
     *
     * @param achievementId 成果ID
     * @return 成果完整信息
     */
    @Override
    public AchievementDetailDTO getAchievementDetail(Long achievementId) {
        log.info("获取成果详情: achievementId={}", achievementId);

        // 1. 查询成果主表信息
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在"));

        // 2. 查询成果详情信息
        AchievementDetail detail = achievementDetailRepository.findByAchievementId(achievementId)
                .orElseThrow(() -> new ServiceException("成果对应的详情不存在"));

        // 3. 查询文件列表
        List<AchievementFileDTO> files = achievementFileService.getFilesByAchievementId(achievementId);

        // 4. 解析详情数据JSON
        Map<String, Object> detailDataMap = parseDetailData(detail.getDetailData());

        // 5. 组装返回DTO
        return AchievementDetailDTO.builder()
                .id(String.valueOf(achievement.getId()))
                .projectId(String.valueOf(achievement.getProjectId()))
                .title(achievement.getTitle())
                .type(achievement.getType())
                .typeName(achievement.getType() != null ? achievement.getType().getName() : "")
                .status(achievement.getStatus())
                .creatorId(String.valueOf(achievement.getCreatorId()))
                .isPublic(achievement.getIsPublic())
                .abstractText(detail.getAbstractText())
                .detailData(detailDataMap)
                .files(files)
                .fileCount(files != null ? files.size() : 0)
                .createdAt(achievement.getCreatedAt())
                .updatedAt(achievement.getUpdatedAt())
                .createdBy(achievement.getCreatedBy())
                .updatedBy(achievement.getUpdatedBy())
                .build();
    }

    /**
     * 创建自定义模板（用于custom类型）
     * 用户可以定义自己的字段结构
     *
     * @param templateDTO 模板定义
     * @return 创建的模板
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AchievementTemplateDTO createCustomTemplate(AchievementTemplateDTO templateDTO) {
        log.info("创建自定义模板: templateName={}", templateDTO.getTemplateName());

        // 1. 校验模板数据
        if (templateDTO.getFields() == null || templateDTO.getFields().isEmpty()) {
            throw new ServiceException("自定义模板至少需要一个字段");
        }

        // 2. 校验字段数量（最多十个）
        if (templateDTO.getFields().size() > 10) {
            throw new ServiceException("自定义模板最多支持10个字段");
        }

        // 3. 验证字段定义
        for (CustomAchievementFieldDTO field : templateDTO.getFields()) {
            // 验证必填属性
            if (StringUtils.isEmpty(field.getFieldKey())) {
                throw new ServiceException("字段键不能为空");
            }
            if (StringUtils.isEmpty(field.getFieldLabel())) {
                throw new ServiceException("字段名称不能为空");
            }
            if (StringUtils.isEmpty(field.getFieldType())) {
                throw new ServiceException("字段类型不能为空");
            }

            // 验证字段类型是否合法
            if (!isValidFieldType(field.getFieldType())) {
                throw new ServiceException("不支持的字段类型: " + field.getFieldType());
            }

            // 如果是select或multiselect类型，必须提供选项
            if (("select".equals(field.getFieldType()) || "multiselect".equals(field.getFieldType()))
                    && (field.getOptions() == null || field.getOptions().length == 0)) {
                throw new ServiceException("选择类型字段必须提供选项: " + field.getFieldLabel());
            }
        }

        // 4. 设置模板属性
        templateDTO.setType(AchievementType.CUSTOM);
        templateDTO.setIsSystem(false);

        // 5. 生成模板ID（使用雪花ID）
        Long templateId = SnowflakeIdUtils.nextId();
        templateDTO.setTemplateId(String.valueOf(templateId));

        // 注意：新架构中没有单独的模板表，模板信息不持久化到数据库
        log.info("自定义模板创建成功: templateId={}", templateDTO.getTemplateId());
        return templateDTO;
    }

    /**
     * 更新成果详情数据
     * 允许更新JSON字段和摘要
     *
     * @param updateDTO 更新DTO
     * @return 更新后的详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AchievementDetailDTO updateDetailData(UpdateDetailDataDTO updateDTO) {
        log.info("更新成果详情数据: achievementId={}", updateDTO.getAchievementId());

        // 1. 查询成果是否存在
        achievementRepository.findById(updateDTO.getAchievementId())
                .orElseThrow(() -> new ServiceException("成果不存在: " + updateDTO.getAchievementId()));

        // 2. 查询详情
        AchievementDetail detail = achievementDetailRepository
                .findByAchievementId(updateDTO.getAchievementId())
                .orElseThrow(() -> new ServiceException("成果详情不存在"));

        // 3. 更新详情数据
        if (updateDTO.getDetailData() != null && !updateDTO.getDetailData().isEmpty()) {
            // 3.1 解析现有数据
            Map<String, Object> currentData = parseDetailData(detail.getDetailData());

            // 3.2 合并数据
            currentData.putAll(updateDTO.getDetailData());

            // 3.3 将Map转换为JSON字符串
            String newDetailDataJson = JsonUtils.toJsonString(currentData);
            if (StringUtils.isEmpty(newDetailDataJson)) {
                throw new ServiceException("详情数据序列化失败");
            }

            // 3.4 保存更新后的JSON
            detail.setDetailData(newDetailDataJson);
            log.debug("更新后的详情数据: {}", newDetailDataJson);
        }

        // 4. 更新摘要
        if (StringUtils.isNotEmpty(updateDTO.getAbstractText())) {
            detail.setAbstractText(updateDTO.getAbstractText());
        }

        // 5. 验证数据合法性
        Map<String, Object> finalData = parseDetailData(detail.getDetailData());
        if (!validateDetailData(updateDTO.getAchievementId(), finalData)) {
            throw new ServiceException("详情数据验证失败，请检查必填字段");
        }

        // 6. 保存
        achievementDetailRepository.save(detail);

        log.info("成果详情更新成功: achievementId={}, fieldsUpdated={}",
                updateDTO.getAchievementId(),
                updateDTO.getDetailData() != null ? updateDTO.getDetailData().size() : 0);

        // 7. 返回更新后的完整详情
        return getAchievementDetail(updateDTO.getAchievementId());
    }

    /**
     * 批量更新详情字段
     * 部分更新，只更新传入的字段
     *
     * @param achievementId 成果ID
     * @param fieldUpdates  字段更新Map
     * @param userId        操作用户ID
     * @return 更新后的详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AchievementDetailDTO updateDetailFields(Long achievementId, Map<String, Object> fieldUpdates, Long userId) {
        log.info("批量更新详情字段: achievementId={}, fieldsCount={}", achievementId, fieldUpdates.size());

        // 1. 查询成果的详情
        AchievementDetail detail = achievementDetailRepository.findByAchievementId(achievementId)
                .orElseThrow(() -> new ServiceException("成果详情不存在"));

        // 2. 解析现有数据
        Map<String, Object> currentData = parseDetailData(detail.getDetailData());

        // 3. 合并更新字段
        currentData.putAll(fieldUpdates);

        // 4. 转换为JSON
        String updatedJson = JsonUtils.toJsonString(currentData);
        if (StringUtils.isEmpty(updatedJson)) {
            throw new ServiceException("字段数据序列化失败");
        }
        detail.setDetailData(updatedJson);

        // 5. 保存
        achievementDetailRepository.save(detail);

        log.info("字段更新成功: achievementId={}, updatedFields={}",
                achievementId, String.join(", ", fieldUpdates.keySet()));

        return getAchievementDetail(achievementId);
    }

    /**
     * 根据模板初始化详情数据
     * 为新成果创建初始的JSON数据结构
     *
     * @param achievementId 成果ID
     * @param type          成果类型
     * @param initialData   初始数据（可选）
     * @return 初始化后的详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AchievementDetailDTO initializeDetailByTemplate(Long achievementId, AchievementType type, Map<String, Object> initialData) {
        log.info("根据模板初始化详情数据: achievementId={}, type={}", achievementId, type);

        // 1. 验证成果是否存在
        achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在"));

        // 2. 检查是否已经存在详情
        if (achievementDetailRepository.findByAchievementId(achievementId).isPresent()) {
            throw new ServiceException("成果详情已存在，无需重新初始化");
        }

        // 3. 获取模板
        AchievementTemplateDTO template = getTemplateByType(type);

        // 4. 初始化数据
        Map<String, Object> detailData = initializeDetailDataFromTemplate(template);

        // 5. 合并用户提供的初始数据
        if (initialData != null && !initialData.isEmpty()) {
            detailData.putAll(initialData);
        }

        // 6. 转换为JSON
        String detailDataJson = JsonUtils.toJsonString(detailData);
        if (StringUtils.isEmpty(detailDataJson)) {
            throw new ServiceException("详情数据序列化失败");
        }

        // 7. 创建详情记录
        AchievementDetail detail = AchievementDetail.builder()
                .id(SnowflakeIdUtils.nextId())
                .achievementId(achievementId)
                .detailData(detailDataJson)
                .abstractText("")
                .build();

        // 8. 持久化
        achievementDetailRepository.save(detail);

        log.info("详情数据初始化成功: achievementId={}", achievementId);

        // 9. 返回完整详情
        return getAchievementDetail(achievementId);
    }

    /**
     * 验证详情数据的合法性
     * 根据成果类型验证必填字段和数据格式
     *
     * @param achievementId 成果ID
     * @param detailData    详情数据
     * @return 验证结果（true=通过，false=不通过）
     */
    @Override
    public boolean validateDetailData(Long achievementId, Map<String, Object> detailData) {
        // 1. 查询成果类型
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ServiceException("成果不存在"));

        // 2. 获取对应模板
        AchievementTemplateDTO template = getTemplateByType(achievement.getType());

        // 3. 验证必填字段
        if (template.getFields() != null) {
            for (CustomAchievementFieldDTO field : template.getFields()) {
                if (field.getRequired() != null && field.getRequired()) {
                    Object value = detailData.get(field.getFieldKey());
                    if (value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
                        log.warn("字段验证失败: 缺少必填字段 {}", field.getFieldKey());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 获取成果类型的预设模板
     * 根据成果类型返回推荐的字段模板
     *
     * @param type 成果类型
     * @return 模板定义
     */
    @Override
    public AchievementTemplateDTO getTemplateByType(AchievementType type) {
        log.info("获取成果类型模板: type={}", type);

        return switch (type) {
            case PAPER -> AchievementTemplate.getPaperTemplate();
            case PATENT -> AchievementTemplate.getPatentTemplate();
            case DATASET -> AchievementTemplate.getDatasetTemplate();
            case MODEL -> AchievementTemplate.getModelTemplate();
            case REPORT -> AchievementTemplate.getReportTemplate();
            case CUSTOM -> AchievementTemplate.getCustomTemplate();
            case TASK_RESULT -> AchievementTemplate.getTaskResultTemplate();
        };
    }

    /**
     * 更新摘要
     *
     * @param achievementId 成果ID
     * @param abstractText  摘要内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAbstract(Long achievementId, String abstractText) {
        log.info("更新摘要: achievementId={}", achievementId);

        AchievementDetail detail = achievementDetailRepository.findByAchievementId(achievementId)
                .orElseThrow(() -> new ServiceException("成果详情不存在"));

        detail.setAbstractText(abstractText);
        achievementDetailRepository.save(detail);

        log.info("更新摘要成功: achievementId={}", achievementId);
    }

    /**
     * 删除成果详情
     *
     * @param achievementId 成果ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDetailByAchievementId(Long achievementId) {
        log.info("删除成果详情: achievementId={}", achievementId);

        int deletedCount = achievementDetailRepository.deleteByAchievementId(achievementId);
        log.info("成果详情删除成功: achievementId={}, deletedCount={}", achievementId, deletedCount);
    }

    /**
     * 构建成果DTO
     */
    private AchievementDTO buildAchievementDTO(Achievement achievement) {
        return AchievementDTO.builder()
                .id(String.valueOf(achievement.getId()))
                .projectId(String.valueOf(achievement.getProjectId()))
                .title(achievement.getTitle())
                .type(achievement.getType())
                .typeName(achievement.getType() != null ? achievement.getType().getName() : "")
                .status(achievement.getStatus())
                .creatorId(String.valueOf(achievement.getCreatorId()))
                .isPublic(achievement.getIsPublic())
                .createdAt(achievement.getCreatedAt())
                .updatedAt(achievement.getUpdatedAt())
                .build();
    }

    /**
     * 根据模板初始化空的详情数据
     * 为字段设置默认值
     *
     * @param template 成果模板
     * @return 初始化的数据Map
     */
    private Map<String, Object> initializeDetailDataFromTemplate(AchievementTemplateDTO template) {
        Map<String, Object> initialData = new HashMap<>();

        if (template.getFields() != null) {
            for (CustomAchievementFieldDTO field : template.getFields()) {
                // 如果字段有默认值，则设置默认值
                if (StringUtils.isNotEmpty(field.getDefaultValue())) {
                    initialData.put(field.getFieldKey(), field.getDefaultValue());
                } else {
                    // 否则设置为null
                    initialData.put(field.getFieldKey(), null);
                }
            }
        }

        return initialData;
    }

    /**
     * 解析详情数据JSON
     * 将JSON字符串转换为Map对象
     *
     * @param detailDataJson JSON字符串
     * @return Map对象
     */
    private Map<String, Object> parseDetailData(String detailDataJson) {
        if (StringUtils.isEmpty(detailDataJson)) {
            log.warn("详情数据为空");
            return new HashMap<>();
        }

        try {
            // 使用 JsonUtils 解析 JSON 为 Map
            Map<String, Object> dataMap = JsonUtils.parseObject(
                    detailDataJson,
                    new TypeReference<>() {
                    }
            );
            return dataMap != null ? dataMap : new HashMap<>();
        } catch (Exception e) {
            log.error("解析详情数据失败: {}", detailDataJson, e);
            throw new ServiceException("解析详情数据失败: " + e.getMessage());
        }
    }

    /**
     * 验证字段类型是否合法
     *
     * @param fieldType 字段类型
     * @return 是否合法
     */
    private boolean isValidFieldType(String fieldType) {
        return "text".equals(fieldType)
                || "number".equals(fieldType)
                || "date".equals(fieldType)
                || "textarea".equals(fieldType)
                || "select".equals(fieldType)
                || "multiselect".equals(fieldType);
    }
}
