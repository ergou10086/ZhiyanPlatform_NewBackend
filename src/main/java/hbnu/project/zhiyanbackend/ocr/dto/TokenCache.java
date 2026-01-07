package hbnu.project.zhiyanbackend.ocr.dto;

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
     * 有效时间
     */
    private long expiresTime;

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresTime;
    }
}
