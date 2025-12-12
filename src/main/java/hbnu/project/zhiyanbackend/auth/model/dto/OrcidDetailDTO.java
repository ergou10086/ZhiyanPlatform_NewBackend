package hbnu.project.zhiyanbackend.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ORCID详细信息DTO
 * 用于存储从ORCID API获取的用户公开信息
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrcidDetailDTO {

    /**
     * ORCID iD
     */
    private String orcidId;

    /**
     * 关键词列表
     */
    private List<String> keywords;

    /**
     * 工作经历列表
     */
    private List<EmploymentInfo> employments;

    /**
     * 教育经历列表
     */
    private List<EducationInfo> educations;

    /**
     * 工作经历信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentInfo {
        /**
         * 组织名称
         */
        private String organization;

        /**
         * 部门名称
         */
        private String department;

        /**
         * 职位/角色
         */
        private String roleTitle;

        /**
         * 开始日期 (格式: YYYY-MM)
         */
        private String startDate;

        /**
         * 结束日期 (格式: YYYY-MM, null表示至今)
         */
        private String endDate;

        /**
         * 组织所在城市
         */
        private String city;

        /**
         * 组织所在国家
         */
        private String country;
    }


    /**
     * 教育经历信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationInfo {
        /**
         * 教育机构名称
         */
        private String organization;

        /**
         * 部门/院系
         */
        private String department;

        /**
         * 开始日期 (格式: YYYY-MM)
         */
        private String startDate;

        /**
         * 结束日期 (格式: YYYY-MM, null表示在读)
         */
        private String endDate;

        /**
         * 机构所在城市
         */
        private String city;

        /**
         * 机构所在国家
         */
        private String country;
    }
}
