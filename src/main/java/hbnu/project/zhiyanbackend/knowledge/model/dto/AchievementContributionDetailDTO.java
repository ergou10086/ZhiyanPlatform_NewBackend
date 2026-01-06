package hbnu.project.zhiyanbackend.knowledge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 成果贡献详细数据DTO
 * 用于返回指定日期的详细贡献信息，包括每个贡献者的贡献次数和具体成果列表
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementContributionDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 该日期的总贡献数
     */
    private Long totalCount;

    /**
     * 贡献者列表
     */
    private List<ContributorDetail> contributors;

    /**
     * 贡献者详细信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributorDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名
         */
        private String username;

        /**
         * 用户姓名
         */
        private String name;

        /**
         * 该用户的贡献次数
         */
        private Long count;

        /**
         * 该用户贡献的成果列表
         */
        private List<AchievementInfo> achievements;
    }

    /**
     * 成果信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 成果ID
         */
        private Long id;

        /**
         * 成果标题
         */
        private String title;

        /**
         * 成果名称（兼容字段）
         */
        private String name;

        /**
         * 成果类型
         */
        private String type;

        /**
         * 创建时间
         */
        private String createdAt;

        /**
         * 时间（兼容字段）
         */
        private String time;
    }
}

