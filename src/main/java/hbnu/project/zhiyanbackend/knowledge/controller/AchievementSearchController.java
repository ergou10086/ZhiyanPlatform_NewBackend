package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.knowledge.model.converter.AchievementConverter;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementQueryDTO;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成果查询搜索接口
 * 负责成果的各种查询、搜索功能
 *
 * @author ErgouTree
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/zhiyan/achievement/search")
@Tag(name = "成果查询搜索", description = "成果的多条件查询、搜索、列表获取等")
public class AchievementSearchController {

    private final AchievementSearchService achievementSearchService;

    private final AchievementRepository achievementRepository;

    private AchievementConverter achievementConverter;

    /**
     * 分页查询成果列表
     * 支持多条件组合查询
     */
    @PostMapping("/query")
    @Operation(summary = "分页查询成果列表", description = "支持多条件组合查询成果")
    public R<Page<AchievementDTO>> queryAchievements(
            @Valid @RequestBody AchievementQueryDTO queryDTO){
        log.info("分页查询成果: queryDTO={}", queryDTO);

        // 构建分页参数
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(queryDTO.getSortOrder())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                queryDTO.getSortBy()
        );

        Pageable pageable = PageRequest.of(queryDTO.getPage(), queryDTO.getSize(), sort);

        Page<AchievementDTO> result = achievementSearchService.queryAchievements(queryDTO, pageable);

        log.info("查询成功: totalElements={}, totalPages={}",
                result.getTotalElements(), result.getTotalPages());
        return R.ok(result, "查询成功");
    }
}
