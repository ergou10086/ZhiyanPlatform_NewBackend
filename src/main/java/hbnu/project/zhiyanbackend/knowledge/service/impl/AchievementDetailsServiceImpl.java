package hbnu.project.zhiyanbackend.knowledge.service.impl;

import hbnu.project.zhiyanbackend.knowledge.model.dto.*;
import hbnu.project.zhiyanbackend.knowledge.model.enums.AchievementType;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementDetailsServiceImpl implements AchievementDetailsService {
    @Override
    public AchievementDTO createAchievementWithDetails(CreateAchievementDTO createDTO) {
        return null;
    }

    @Override
    public AchievementDetailDTO getAchievementDetail(Long achievementId) {
        return null;
    }

    @Override
    public AchievementTemplateDTO createCustomTemplate(AchievementTemplateDTO templateDTO) {
        return null;
    }

    @Override
    public AchievementDetailDTO updateDetailData(UpdateDetailDataDTO updateDTO) {
        return null;
    }

    @Override
    public AchievementDetailDTO updateDetailFields(Long achievementId, Map<String, Object> fieldUpdates, Long userId) {
        return null;
    }

    @Override
    public AchievementDetailDTO initializeDetailByTemplate(Long achievementId, AchievementType type, Map<String, Object> initialData) {
        return null;
    }

    @Override
    public boolean validateDetailData(Long achievementId, Map<String, Object> detailData) {
        return false;
    }

    @Override
    public List<AchievementTemplateDTO> getAllSystemTemplates() {
        return List.of();
    }

    @Override
    public AchievementTemplateDTO getTemplateByType(AchievementType type) {
        return null;
    }

    @Override
    public void updateAbstract(Long achievementId, String abstractText) {

    }

    @Override
    public void deleteDetailByAchievementId(Long achievementId) {

    }
}
