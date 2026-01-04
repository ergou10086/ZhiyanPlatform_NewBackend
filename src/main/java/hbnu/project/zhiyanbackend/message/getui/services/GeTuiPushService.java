package hbnu.project.zhiyanbackend.message.getui.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import hbnu.project.zhiyanbackend.message.getui.config.GeTuiConfig;
import hbnu.project.zhiyanbackend.message.getui.dto.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 个推消息推送服务
 * 提供各种推送方式的实现
 *
 * @author ErgouTree
 */
@Slf4j
@Service
public class GeTuiPushService {

    @Resource
    private GeTuiTokenService tokenService;

    @Resource
    private GeTuiConfig geTuiConfig;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;


    /**
     * 单推 - 根据CID推送
     * 参数示例
     * {
     *   "request_id": "xxx",
     *   "settings": {
     *     "ttl": 7200000
     *   },
     *   "audience": {
     *     "cid": [
     *       "xxx"
     *     ]
     *   },
     *   "push_message": {
     *     "notification": {
     *       "title": "请填写通知标题",
     *       "body": "请填写通知内容",
     *       "click_type": "url",
     *       "url": "https//:xxx"
     *     }
     *   }
     * }
     *
     * @param cid
     * @param title
     * @param body
     * @param url
     * @return
     */
    public PushResponse pushSingleByCid(String cid, String title, String body, String url) {
        return pushSingleByCid(cid, title, body, url, null);
    }

    /**
     * 单推 - 根据CID推送
     * 带自定义参数
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-1
     *
     * @param cid cid https://docs.getui.com/getui/more/word/?id=doc-title-4
     * @param title 消息标题
     * @param body 消息内容
     * @param url 需要跳转的外部url
     * @param customParams 自定义参数
     * @return PushResponse
     */
    public PushResponse pushSingleByCid(String cid, String title, String body, String url, Map<String, Object> customParams) {
        String requestId = generateRequestId();

        // 构建推送消息
        Notification notification = Notification.builder()
                .title(title)
                .body(body)
                .clickType("url")
                .url(url)
                .build();

        PushMessage pushMessage = PushMessage.builder()
                .notification(notification)
                .build();

        // 构建推送设置
        PushSettings settings = PushSettings.builder()
                // 2小时
                .ttl(7200000L)
                .strategy(Strategy.builder().defaultStrategy(1).build())
                .build();

        // 构建目标用户
        Audience audience = Audience.builder()
                .cid(Collections.singletonList(cid))
                .build();

        // 构建请求
        PushMessageRequest request = PushMessageRequest.builder()
                .requestId(requestId)
                .settings(settings)
                .audience(audience)
                .pushMessage(pushMessage)
                .build();

        // 如果有自定义参数，添加厂商通道配置
        if (customParams != null && !customParams.isEmpty()) {
            request.setPushChannel(buildPushChannel(title, body, url));
        }

        // https://docs.getui.com/getui/server/rest_v2/push/#doc-title-1
        String apiUrl = geTuiConfig.getFullBaseUrl() + "/push/single/cid";
        return executeRequest(apiUrl, request, PushResponse.class);
    }


    /**
     * 批量单推 - 根据CID
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-3
     * 参数示例
     * {
     *   "is_async": true,
     *   "msg_list": [
     *     {
     *       "request_id": "",
     *       "settings": {
     *         "ttl": 7200000
     *       },
     *       "audience": {
     *         "cid": [
     *           "xxxx"
     *         ]
     *       },
     *       "push_message": {
     *         "notification": {
     *           "title": "请填写通知标题",
     *           "body": "请填写通知内容",
     *           "click_type": "url",
     *           "url": "https://xxx"
     *         }
     *       }
     *     }
     *   ]
     * }
     *
     * @param messages 消息队列
     * @param isAsync 是否异步
     * @return PushResponse
     */
    public PushResponse pushBatchByCid(List<PushMessageRequest> messages, boolean isAsync) {
        BatchPushRequest request = BatchPushRequest.builder()
                .isAsync(isAsync)
                .msgList(messages)
                .build();

        // https://docs.getui.com/getui/server/rest_v2/push/#doc-title-3
        String apiUrl = geTuiConfig.getFullBaseUrl() + "/push/single/batch/cid";
        return executeRequest(apiUrl, request, PushResponse.class);
    }


    /**
     * 创建批量推送消息（toList的第一步）
     * 此接口用来创建消息体，并返回taskid，为批量推的前置步骤
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-4
     * 请求示例
     * {
     *   "group_name": "请填写任务组名",
     *   "settings": {
     *     "ttl": 7200000
     *   },
     *   "push_message": {
     *     "notification": {
     *       "title": "请填写通知标题",
     *       "body": "请填写通知内容",
     *       "click_type": "url",
     *       "url": "https//:xxx"
     *     }
     *   }
     * }
     * 响应示例
     * {
     *   "code": 0,
     *   "msg": "",
     *   "data": {
     *     "taskid": ""
     *   }
     * }
     *
     * @param title 消息标题
     * @param body 消息体
     * @param url 跳转的url
     * @param groupName 请填写任务组名
     * @return
     */
    public String createListMessage(String title, String body, String url, String groupName) {
        Notification notification = Notification.builder()
                .title(title)
                .body(body)
                .clickType("url")
                .url(url)
                .build();

        PushMessage pushMessage = PushMessage.builder()
                .notification(notification)
                .build();

        PushSettings settings = PushSettings.builder()
                .ttl(7200000L)
                // 先走默认，这个东西还是不要自己设置了
                // .strategy(Strategy.builder().defaultStrategy(1).build())
                .build();

        Map<String, Object> request = new HashMap<>();
        if (groupName != null && !groupName.isEmpty()) {
            request.put("group_name", groupName);
        }
        request.put("settings", settings);
        request.put("push_message", pushMessage);
        request.put("push_channel", buildPushChannel(title, body, url));

        // https://docs.getui.com/getui/server/rest_v2/push/
        // 接口地址: BaseUrl/push/list/message
        String apiUrl = geTuiConfig.getFullBaseUrl() + "/push/list/message";
        CreateMessageResponse response = executeRequest(apiUrl, request, CreateMessageResponse.class);

        if(response != null && response.getData() != null) {
            return response.getData().getTaskid();
        }

        throw new RuntimeException("创建批量推送消息失败");
    }


    /**
     * cid批量推送
     * 执行批量推送 - 根据CID列表
     * 接口地址: BaseUrl/push/list/cid
     * https://docs.getui.com/getui/server/rest_v2/push/
     * 请求体示例
     * {
     *   "audience": {
     *     "cid": [
     *       "xxxx1",
     *       "xxxx2"
     *     ]
     *   },
     *   "taskid": "",
     *   "is_async": true
     * }
     * 返回体示例
     * {
     *   "code": 0,
     *   "msg": "",
     *   "data": {
     *     "$taskid": {
     *       "$cid": "$status"
     *     }
     *   }
     * }
     *
     * @param taskId 使用创建消息接口返回的taskId，可以多次使用
     * @param cidList cid数组，数组长度不大于200
     * @param isAsync 是否异步
     * @return PushResponse
     */
    public PushResponse executePushListByCid(String taskId, List<String> cidList, boolean isAsync) {
        // 推送目标用户体
        Audience audience = Audience.builder()
                .cid(cidList)
                .build();

        BatchPushExecuteRequest request = BatchPushExecuteRequest.builder()
                .audience(audience)
                .taskid(taskId)
                .isAsync(isAsync)
                .build();

        String apiUrl = geTuiConfig.getFullBaseUrl() + "/push/list/cid";
        return executeRequest(apiUrl, request, PushResponse.class);
    }


    /**
     * 群推
     * 对指定应用的所有用户群发推送消息
     *
     * @param title 消息标题
     * @param body 消息体
     * @param url 跳转的url
     * @param groupName 请填写任务组名
     * @return
     */
    public PushResponse pushToAll(String title, String body, String url, String groupName) {
        return pushToAll(title, body, url, groupName, null, null);
    }


    /**
     * 群推 - 推送给所有用户
     * 支持定时和定速
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-8
     * 请求体示例
     * {
     *   "request_id": "请填写requestid",
     *   "group_name": "请填写任务组名",
     *   "settings": {
     *     "ttl": 7200000
     *   },
     *   "audience": "all",
     *   "push_message": {
     *     "notification": {
     *       "title": "请填写通知标题",
     *       "body": "请填写通知内容",
     *       "click_type": "url",
     *       "url": "https//:xxx"
     *     }
     *   }
     * }
     *
     * @param title 消息标题
     * @param body  消息体
     * @param url   消息需跳转的url
     * @param groupName   任务组名
     * @param scheduleTime   定时
     * @param speed   定速
     * @return  PushResponse
     */
    public PushResponse pushToAll(String title, String body, String url, String groupName,
                                  Long scheduleTime, Integer speed) {
        String requestId = generateRequestId();

        Notification notification = Notification.builder()
                .title(title)
                .body(body)
                .clickType("url")
                .url(url)
                .build();

        PushMessage pushMessage = PushMessage.builder()
                .notification(notification)
                .build();

        PushSettings.PushSettingsBuilder settingsBuilder = PushSettings.builder()
                .ttl(7200000L)
                .strategy(Strategy.builder().defaultStrategy(1).build());
        if (scheduleTime != null) {
            settingsBuilder.scheduleTime(scheduleTime);
        }
        if (speed != null) {
            settingsBuilder.speed(speed);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("request_id", requestId);
        if (groupName != null && !groupName.isEmpty()) {
            request.put("group_name", groupName);
        }
        request.put("settings", settingsBuilder.build());
        request.put("audience", "all");
        request.put("push_message", pushMessage);
        request.put("push_channel", buildPushChannel(title, body, url));

        String apiUrl = geTuiConfig.getFullBaseUrl() + "/push/all";
        return executeRequest(apiUrl, request, PushResponse.class);
    }


    /**
     * 停止任务
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-11
     */
    public void stopTask(String taskId) {
        String apiUrl = geTuiConfig.getFullBaseUrl() + "/task/" + taskId;
        executeDeleteRequest(apiUrl);
    }

    /**
     * 查询定时任务
     * 该接口支持在推送完定时任务之后，查看定时任务状态，定时任务是否发送成功。
     * 创建定时任务请见接口执行群推
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-12
     * 返回结构：https://docs.getui.com/getui/server/rest_v2/common_args/?id=doc-title-1
     */
    public ScheduleTaskResponse queryScheduleTask(String taskId) {
        String apiUrl = geTuiConfig.getFullBaseUrl() + "/task/schedule/" + taskId;
        return executeGetRequest(apiUrl, ScheduleTaskResponse.class);
    }

    /**
     * 删除定时任务
     * 用来删除还未下发的任务，删除后定时任务不再触发
     * 距离下发还有一分钟的任务，将无法删除
     * https://docs.getui.com/getui/server/rest_v2/push/#doc-title-13
     */
    public void deleteScheduleTask(String taskId) {
        String apiUrl = geTuiConfig.getFullBaseUrl() + "/task/schedule/" + taskId;
        executeDeleteRequest(apiUrl);
    }

    // ---------------------------------------------------------------------

    /**
     * 构建厂商推送通道配置
     */
    private PushChannel buildPushChannel(String title, String body, String url) {
        // Android厂商配置
        UpsNotification upsNotification = UpsNotification.builder()
                .title(title)
                .body(body)
                .clickType("url")
                .url(url)
                .build();

        Ups ups = Ups.builder()
                .notification(upsNotification)
                .build();

        AndroidChannel androidChannel = AndroidChannel.builder()
                .ups(ups)
                .build();

        return PushChannel.builder()
                .android(androidChannel)
                .build();
    }

    /**
     * 执行HTTP POST请求
     */
    private <T, R> R executeRequest(String url, T request, Class<R> responseType) {
        return executeRequest(url, request, responseType, 0);
    }

    private <T, R> R executeRequest(String url, T request, Class<R> responseType, int retryCount) {
        try {
            String token = tokenService.getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", token);

            HttpEntity<T> httpRequest = new HttpEntity<>(request, headers);

            log.info("发送推送请求: url={}", url);
            ResponseEntity<R> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    responseType
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("请求失败: {}", responseBody);

            // 检查是否是token过期错误（错误码10001）
            if (responseBody.contains("\"code\":10001") && retryCount == 0) {
                log.warn("token过期，尝试刷新后重试");
                tokenService.refreshTokenOnError();
                return executeRequest(url, request, responseType, retryCount + 1);
            }

            throw new RuntimeException("推送请求失败: " + responseBody, e);
        } catch (Exception e) {
            log.error("推送请求异常", e);
            throw new RuntimeException("推送请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 执行HTTP GET请求
     */
    private <R> R executeGetRequest(String url, Class<R> responseType) {
        try {
            String token = tokenService.getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("token", token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<R> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    responseType
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("GET请求异常", e);
            throw new RuntimeException("GET请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 执行HTTP DELETE请求
     */
    private void executeDeleteRequest(String url) {
        try {
            String token = tokenService.getToken();

            HttpHeaders headers = new HttpHeaders();
            headers.set("token", token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );

            log.info("删除请求成功: url={}", url);

        } catch (Exception e) {
            log.error("DELETE请求异常", e);
            throw new RuntimeException("DELETE请求异常: " + e.getMessage(), e);
        }
    }


    /**
     * 生成请求ID（10-32位）
     */
    private String generateRequestId() {
        return "REQ_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}

