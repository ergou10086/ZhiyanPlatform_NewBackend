package hbnu.project.zhiyanbackend.message.getui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 推送目标用户
 * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-1
 *
 * @author ErgouTree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audience {

    /**
     * CID数组
     * cid数组，只能填一个cid
     * 单推用的
     * 个推业务层中的对外用户标识，用于标识客户端身份，由第三方客户端获取并保存到第三方服务端，是个推SDK的唯一识别号,简称CID。
     * 参考 https://docs.getui.com/getui/more/word/?id=doc-title-4
     */
    private List<String> cid;
    
    /**
     * 别名数组
     */
    private List<String> alias;
    
    /**
     * 标签条件
     */
    private List<Tag> tag;
    
    /**
     * 快速标签
     */
    @JsonProperty("fast_custom_tag")
    private String fastCustomTag;
}