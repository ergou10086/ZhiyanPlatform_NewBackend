package hbnu.project.zhiyanbackend.basic.utils.ip;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * IP定位工具类
 * 使用hutool进行IP地址定位
 *
 * @author ErgouTree
 */
@Slf4j
public class IpLocationUtils {

    /**
     * IP定位API地址（使用免费的IP定位服务）
     * 可以使用多个服务商，这里使用ip-api.com（免费，无需API Key）
     */
    private static final String IP_API_URL = "http://ip-api.com/json/";

    /**
     * 根据IP地址获取地理位置信息
     *
     * @param ip IP地址
     * @return 地理位置信息字符串，格式：国家 省份 城市，如果获取失败返回IP地址
     */
    public static String getLocationByIp(String ip) {
        if (StrUtil.isBlank(ip) || "unknown".equals(ip) || "127.0.0.1".equals(ip)) {
            return "本地";
        }

        try {
            // 调用IP定位API
            String response = HttpUtil.get(IP_API_URL + ip + "?lang=zh-CN", 3000);
            
            if (StrUtil.isBlank(response)) {
                log.warn("IP定位API返回为空: ip={}", ip);
                return ip;
            }

            JSONObject json = JSONUtil.parseObj(response);
            
            // 检查API返回状态
            String status = json.getStr("status");
            if (!"success".equals(status)) {
                log.warn("IP定位失败: ip={}, status={}", ip, status);
                return ip;
            }

            // 提取地理位置信息
            String country = json.getStr("country", "");
            String regionName = json.getStr("regionName", "");
            String city = json.getStr("city", "");

            // 构建位置字符串
            StringBuilder location = new StringBuilder();
            if (StrUtil.isNotBlank(country)) {
                location.append(country);
            }
            if (StrUtil.isNotBlank(regionName)) {
                if (!location.isEmpty()) {
                    location.append(" ");
                }
                location.append(regionName);
            }
            if (StrUtil.isNotBlank(city)) {
                if (!location.isEmpty()) {
                    location.append(" ");
                }
                location.append(city);
            }

            return !location.isEmpty() ? location.toString() : ip;

        } catch (Exception e) {
            log.error("IP定位异常: ip={}", ip, e);
            return ip;
        }
    }

    /**
     * 判断两个IP地址是否不同（简单的字符串比较）
     * 注意：这里只是简单的字符串比较，实际应用中可能需要更复杂的判断逻辑
     *
     * @param ip1 IP地址1
     * @param ip2 IP地址2
     * @return 是否不同
     */
    public static boolean isDifferentIp(String ip1, String ip2) {
        if (StrUtil.isBlank(ip1) || StrUtil.isBlank(ip2)) {
            return false;
        }
        
        // 如果是本地IP，不视为不同
        if ("127.0.0.1".equals(ip1) || "127.0.0.1".equals(ip2) ||
            "0:0:0:0:0:0:0:1".equals(ip1) || "0:0:0:0:0:0:0:1".equals(ip2)) {
            return false;
        }

        return !ip1.equals(ip2);
    }
}