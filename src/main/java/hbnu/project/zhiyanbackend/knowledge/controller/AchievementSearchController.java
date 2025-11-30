package hbnu.project.zhiyanbackend.knowledge.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ControllerException;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.ValidationUtils;
import hbnu.project.zhiyanbackend.knowledge.model.converter.AchievementConverter;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementDTO;
import hbnu.project.zhiyanbackend.knowledge.model.dto.AchievementQueryDTO;
import hbnu.project.zhiyanbackend.knowledge.model.entity.Achievement;
import hbnu.project.zhiyanbackend.knowledge.repository.AchievementRepository;
import hbnu.project.zhiyanbackend.knowledge.service.AchievementSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    private final AchievementConverter achievementConverter;

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
        Pageable pageable = buildPageable(queryDTO.getPage(), queryDTO.getSize(),
                queryDTO.getSortBy(), queryDTO.getSortOrder());

        Page<AchievementDTO> result = achievementSearchService.queryAchievements(queryDTO, pageable);

        log.info("查询成功: totalElements={}, totalPages={}",
                result.getTotalElements(), result.getTotalPages());
        return R.ok(result, "查询成功");
    }

    /**
     * 根据项目ID查询成果列表
     * 简化查询，只需要项目ID
     */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "根据项目ID查询成果列表", description = "查询指定项目下的所有成果")
    public R<Page<AchievementDTO>> queryAchievementsByProject(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") String sortOrder){
        log.info("根据项目ID查询成果: projectId={}, page={}, size={}", projectId, page, size);

        // 参数校验
        ValidationUtils.requireId(projectId, "项目ID");

        // 构建分页参数
        Pageable pageable = buildPageable(page, size, sortBy, sortOrder);

        Page<AchievementDTO> result = achievementSearchService.getAchievementsByProjectId(projectId, pageable);

        return R.ok(result, "查询成功");
    }

    /**
     * 根据成果名称查询（模糊匹配）
     */
    @GetMapping("/search/name/{achievementName}")
    @Operation(summary = "根据成果名称查询", description = "模糊匹配成果标题")
    public R<AchievementDTO> getAchievementByName(
            @Parameter(description = "成果名称", required = true) @PathVariable String achievementName) {
        log.info("根据名称查询成果: achievementName={}", achievementName);

        // 参数校验
        if (achievementName.trim().isEmpty()) {
            throw new ControllerException("成果名称不能为空");
        }

        AchievementDTO result = achievementSearchService.getAchievementByName(achievementName);

        return result != null ? R.ok(result, "查询成功") : R.fail("未找到匹配的成果");
    }

    /**
     * 批量查询成果信息
     * 只返回公开的成果
     */
    @GetMapping("/batch")
    @Operation(summary = "批量查询成果", description = "根据ID列表批量查询成果信息")
    public R<List<AchievementDTO>> getAchievementsByIds(
            @RequestParam("ids") String ids) {
        log.info("批量查询成果: ids={}", ids);

        // 参数校验
        if (ids == null || ids.trim().isEmpty()) {
            throw new ControllerException("ID列表不能为空");
        }

        List<Long> achievementIds =  Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();

        List<Achievement> achievements = achievementRepository.findAllById(achievementIds);

        // 只返回该项目中公开的成果
        List<AchievementDTO> dtoList = achievements.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsPublic()))
                .map(achievementConverter::toDTO)
                .collect(Collectors.toList());

        return R.ok(dtoList, "查询成功");
    }

    /**
     * 组合搜索
     * 根据关键字搜索成果（搜索标题、摘要等字段）
     */
    @GetMapping("/search")
    @Operation(summary = "组合搜索", description = "根据关键字搜索成果")
    public R<Page<AchievementDTO>> searchAchievements(
            @Parameter(description = "搜索关键字") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        log.info("组合搜索成果: keyword={}", keyword);

        // 参数校验
        if (keyword.trim().isEmpty()) {
            throw new ServiceException("搜索关键字不能为空");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<AchievementDTO> result = achievementSearchService.combinationSearch(keyword, pageable);

        return R.ok(result, "搜索成功");
    }

    /**
     * 构建分页参数
     * @param page 页码
     * @param size 每页数量
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 分页对象
     */
    private Pageable buildPageable(Integer page, Integer size, String sortBy, String sortOrder) {
        // 页码校验（不能为负数）
        int validPage = Objects.nonNull(page) ? Math.max(page, 0) : 0;

        // 每页数量校验（1-20之间）
        int validSize = Objects.nonNull(size) ? Math.min(Math.max(size, 1), 20) : 10;

        // 排序方向处理
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 排序字段处理（默认按创建时间排序）
        String validSortBy = Objects.nonNull(sortBy) && !sortBy.trim().isEmpty()
                ? sortBy
                : "createdAt";

        return PageRequest.of(validPage, validSize, Sort.by(direction, validSortBy));
    }
}
