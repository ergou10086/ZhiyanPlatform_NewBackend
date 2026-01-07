package hbnu.project.zhiyanbackend.message.getui.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.message.getui.config.GeTuiConfig;
import hbnu.project.zhiyanbackend.message.getui.dto.AuthRequest;
import hbnu.project.zhiyanbackend.message.getui.dto.AuthResponse;
import hbnu.project.zhiyanbackend.redis.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 个推Token管理服务
 * 负责token的获取、刷新和缓存管理
 * unipush1.0
 *
 * @author ErgouTree
 */
@Deprecated
@Slf4j
@Service
public class GeTuiTokenService {

    @Resource
    private GeTuiConfig geTuiConfig;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private RedisService redisService;

    private static final String TOKEN_CACHE_KEY = "getui:token";

    private static final String TOKEN_EXPIRE_KEY = "getui:token:expire";


    /**
     * 优先从 Redis 读取 Token
     */
    public String getToken() {
        // 先从 Redis 获取
        String token = redisService.getCacheObject(TOKEN_CACHE_KEY);
        String expireTime = redisService.getCacheObject(TOKEN_EXPIRE_KEY);

        // 检查token是否有效（提前5分钟刷新）
        if(token != null && expireTime != null){
            long expire = Long.parseLong(expireTime);
            long now = System.currentTimeMillis();
            if (expire - now > 5 * 60 * 1000) {
                log.debug("使用缓存的token");
                return token;
            }
        }

        // token不存在或即将过期，重新获取
        log.info("token不存在或即将过期，重新获取");
        return refreshToken();
    }


    /**
     * 主动刷新token
     */
    public String refreshToken() {
        try{
            // 生成签名
            String timestamp = String.valueOf(System.currentTimeMillis());
            String sign = generateSign(timestamp);

            // 构建请求
            AuthRequest authRequest = AuthRequest.builder()
                    .sign(sign)
                    .timestamp(timestamp)
                    .appkey(geTuiConfig.getAppKey())
                    .build();

            // https://docs.getui.com/getui/server/rest_v2/token/ 中的 获取鉴权token
            String url = geTuiConfig.getFullBaseUrl() + "/auth";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AuthRequest> request = new HttpEntity<>(authRequest, headers);

            // 发送请求
            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AuthResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AuthResponse authResponse = response.getBody();

                // https://docs.getui.com/getui/server/rest_v2/code/?id=doc-title-1 中 对于基础返回码的说明，0是成功，1是失败
                if(authResponse.getCode() == 0 && authResponse.getData() != null) {
                    // 对劲，开始缓存
                    String token = authResponse.getData().getToken();
                    String expireTime = authResponse.getData().getExpireTime();

                    // 缓存 Token 到 Redis
                    long expire = Long.parseLong(expireTime);
                    long now = System.currentTimeMillis();
                    long ttl = (expire - now) / 1000;

                    redisService.setCacheObject(TOKEN_CACHE_KEY, token, ttl, TimeUnit.SECONDS);
                    redisService.setCacheObject(TOKEN_EXPIRE_KEY, expireTime, ttl, TimeUnit.SECONDS);

                    log.info("token刷新成功，过期时间: {}", expireTime);
                    return token;
                }else{
                    log.error("获取token失败: code={}, msg={}", authResponse.getCode(), authResponse.getMsg());
                    throw new RuntimeException("获取token失败: " + authResponse.getMsg());
                }
            } else {
                log.error("获取token失败: HTTP状态码={}", response.getStatusCode());
                throw new RuntimeException("获取token失败");
            }
        } catch (Exception e) {
            log.error("获取token异常", e);
            throw new RuntimeException("获取token异常: " + e.getMessage(), e);
        }
    }


    /**
     * 主动失效 Token，就是删除
     * 为防止token被滥用或泄露，开发者可以调用此接口主动使token失效
     */
    public void deleteToken(String token) {
        try{
            // https://docs.getui.com/getui/server/rest_v2/token/ 中 接口地址 BaseUrl/auth/$token
            String url = geTuiConfig.getFullBaseUrl() + "/auth" + token;

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<AuthRequest> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // 清除缓存
                redisService.deleteObject(TOKEN_CACHE_KEY);
                redisService.deleteObject(TOKEN_EXPIRE_KEY);
                log.info("token删除成功");
            }
        }catch (ServiceException e){
            log.error("删除token异常", e);
            throw new RuntimeException("删除token异常: " + e.getMessage(), e);
        }
    }


    /**
     * 被动刷新 Token
     * 当业务接口返回 10001 错误码的时候
     * https://docs.getui.com/getui/server/rest_v2/code/?id=doc-title-1 中  10001 token错误/失效 调用接口重新获取token
     */
    public String refreshTokenOnError() {
        log.warn("检测到token过期（错误码10001），执行被动刷新");
        // 先清除缓存
        redisService.deleteObject(TOKEN_CACHE_KEY);
        redisService.deleteObject(TOKEN_EXPIRE_KEY);
        // 重新获取
        return refreshToken();
    }


    /**
     * 生成签名
     * sign = SHA256(appkey + timestamp + mastersecret)、
     * https://docs.getui.com/getui/server/rest_v2/token/ 中 生成 sign 值：将 appkey、timestamp、mastersecret 对应的字符串按此固定顺序拼接后，使用 SHA256 算法加密。
     */
    private String generateSign(String timestamp) {
        try {
            String source = geTuiConfig.getAppKey() + timestamp + geTuiConfig.getMasterSecret();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));

            // 转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            log.error("生成签名异常", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
}
