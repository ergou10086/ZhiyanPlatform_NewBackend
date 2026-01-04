package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 标签条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    /**
     * 查询条件：phone_type(手机类型), region(省市), custom_tag(用户标签), portrait(个推用户画像)
     */
    private String key;
    
    /**
     * 查询条件值列表
     */
    private List<String> values;
    
    /**
     * 操作类型：or(或), and(与), not(非)
     */
    @JsonProperty("opt_type")
    private String optType;
}