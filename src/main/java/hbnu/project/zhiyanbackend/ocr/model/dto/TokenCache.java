package hbnu.project.zhiyanbackend.ocr.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token缓存类
 *
 * @author ErgouTree
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenCache {

    /**
     * accessToken
     */
    private String accessToken;

    /**
     * 过期时间戳（毫秒）
     */
    private long expiresTime;

    /**
     * 检查token是否过期
     *
     * @return true表示已过期，false表示未过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresTime;
    }
}
