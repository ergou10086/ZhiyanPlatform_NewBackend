package hbnu.project.zhiyanbackend.knowledge.repository;

import hbnu.project.zhiyanbackend.knowledge.model.entity.AchievementFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 成果文件数据访问层
 * 管理成果关联的文件信息，适配腾讯云COS单桶策略
 *
 * @author ErgouTree
 */
@Repository
public interface AchievementFileRepository extends JpaRepository<AchievementFile, Long>, JpaSpecificationExecutor<AchievementFile> {

    /**
     * 根据成果ID查询所有文件（分页）
     *
     * @param achievementId 成果ID
     * @param pageable      分页参数
     * @return 文件分页列表
     */
    Page<AchievementFile> findByAchievementId(Long achievementId, Pageable pageable);

    /**
     * 根据成果ID查询所有文件
     *
     * @param achievementId 成果ID
     * @return 文件列表
     */
    List<AchievementFile> findByAchievementId(Long achievementId);

    /**
     * 根据成果ID和文件名查询文件
     *
     * @param achievementId 成果ID
     * @param fileName      文件名
     * @return 最新版本文件
     */
    Optional<AchievementFile> findByAchievementIdAndFileName(Long achievementId, String fileName);

    /**
     * 根据上传者ID查询文件列表（分页）
     *
     * @param uploadBy 上传者ID
     * @param pageable 分页参数
     * @return 文件分页列表
     */
    Page<AchievementFile> findByUploadBy(Long uploadBy, Pageable pageable);

    /**
     * 统计成果的文件数量
     *
     * @param achievementId 成果ID
     * @return 文件数量
     */
    long countByAchievementId(Long achievementId);

    /**
     * 删除该成果的所有文件
     *
     * @param achievementId 成果ID
     */
    @Modifying
    @Transactional
    void deleteByAchievementId(Long achievementId);

    /**
     * 根据COS对象键查询文件
     * 新架构使用单桶策略，通过objectKey即可唯一标识文件
     *
     * @param objectKey COS对象键
     * @return 文件
     */
    Optional<AchievementFile> findByObjectKey(String objectKey);

    /**
     * 检查文件是否存在
     *
     * @param achievementId 成果ID
     * @param fileName      文件名
     * @return 是否存在
     */
    boolean existsByAchievementIdAndFileName(Long achievementId, String fileName);

    /**
     * 根据IDs，批量查询成果文件
     *
     * @param achievementIds 成果ID列表
     * @return 文件列表
     */
    @Query("SELECT f FROM AchievementFile f WHERE f.achievementId IN :achievementIds ")
    List<AchievementFile> findLatestFilesByAchievementIdIn(@Param("achievementIds") List<Long> achievementIds);

    /**
     * 根据文件类型查询文件（分页）
     *
     * @param fileType 文件类型
     * @param pageable 分页参数
     * @return 文件分页列表
     */
    Page<AchievementFile> findByFileType(String fileType, Pageable pageable);

    /**
     * 统计指定类型的文件数量
     *
     * @param fileType 文件类型
     * @return 文件数量
     */
    long countByFileType(String fileType);
}
