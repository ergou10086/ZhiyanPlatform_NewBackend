package hbnu.project.zhiyanbackend.message.getui.controller;

import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.message.getui.dto.PushResponse;
import hbnu.project.zhiyanbackend.message.getui.dto.ScheduleTaskResponse;
import hbnu.project.zhiyanbackend.message.getui.services.GeTuiPushService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推送消息控制器
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/message/getui/push")
@Tag(name = "个推消息推送接口")
@Validated
public class GeTuiPushController {

    @Resource
    private GeTuiPushService pushService;

    /**
     * 单个用户推送
     * 通过CID
     */
    @PostMapping("/single/cid")
    @Operation(summary = "单个用户推送-CID")
    public R<PushResponse> pushSingleByCid(
            @Parameter(description = "用户CID") @RequestParam @NotBlank String cid,
            @Parameter(description = "通知标题") @RequestParam @NotBlank String title,
            @Parameter(description = "通知内容") @RequestParam @NotBlank String body,
            @Parameter(description = "点击跳转URL") @RequestParam @NotBlank String url) {
        log.info("单推-CID: cid={}, title={}", cid, title);
        PushResponse result = pushService.pushSingleByCid(cid, title, body, url);
        return R.ok(result);
    }


    /**
     * 批量推送（通过CID列表）- 两步推送法
     */
    @PostMapping("/list/cid")
    @Operation(summary = "批量推送-CID列表")
    public R<PushResponse> pushListByCid(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CID列表")
            @RequestBody @NotEmpty List<String> cidList,
            @Parameter(description = "通知标题") @RequestParam @NotBlank String title,
            @Parameter(description = "通知内容") @RequestParam @NotBlank String body,
            @Parameter(description = "点击跳转URL") @RequestParam @NotBlank String url,
            @Parameter(description = "任务组名") @RequestParam(required = false) String groupName,
            @Parameter(description = "是否异步") @RequestParam(defaultValue = "false") boolean isAsync) {

        log.info("批量推送-CID: cidList.size={}, title={}", cidList.size(), title);

        String taskId = pushService.createListMessage(title, body, url, groupName);
        log.info("创建消息成功，taskId={}", taskId);

        PushResponse result = pushService.executePushListByCid(taskId, cidList, isAsync);
        return R.ok(result);
    }


    /**
     * 群推 - 推送给所有用户
     */
    @PostMapping("/all")
    @Operation(summary = "群推-所有用户")
    public R<PushResponse> pushToAll(
            @Parameter(description = "通知标题") @RequestParam @NotBlank String title,
            @Parameter(description = "通知内容") @RequestParam @NotBlank String body,
            @Parameter(description = "点击跳转URL") @RequestParam @NotBlank String url,
            @Parameter(description = "任务组名") @RequestParam(required = false) String groupName,
            @Parameter(description = "定时推送时间戳(毫秒)") @RequestParam(required = false) Long scheduleTime,
            @Parameter(description = "定速推送(条/秒)") @RequestParam(required = false) Integer speed) {

        log.info("群推: title={}, scheduleTime={}, speed={}", title, scheduleTime, speed);
        PushResponse result = pushService.pushToAll(title, body, url, groupName, scheduleTime, speed);
        return R.ok(result);
    }


    /**
     * 停止推送任务
     */
    @DeleteMapping("/task/{taskId}")
    @Operation(summary = "停止推送任务")
    public R<Void> stopTask(
            @Parameter(description = "任务ID", required = true)
            @PathVariable @NotBlank String taskId) {
        log.info("停止任务: taskId={}", taskId);
        pushService.stopTask(taskId);
        return R.ok(null);
    }

    /**
     * 查询定时任务
     */
    @GetMapping("/schedule/{taskId}")
    @Operation(summary = "查询定时任务")
    public R<ScheduleTaskResponse> queryScheduleTask(
            @Parameter(description = "任务ID", required = true)
            @PathVariable @NotBlank String taskId) {
        log.info("查询定时任务: taskId={}", taskId);
        ScheduleTaskResponse result = pushService.queryScheduleTask(taskId);
        return R.ok(result);
    }

    /**
     * 删除定时任务
     */
    @DeleteMapping("/schedule/{taskId}")
    @Operation(summary = "删除定时任务")
    public R<Void> deleteScheduleTask(
            @Parameter(description = "任务ID", required = true)
            @PathVariable @NotBlank String taskId) {
        log.info("删除定时任务: taskId={}", taskId);
        pushService.deleteScheduleTask(taskId);
        return R.ok(null);
    }
}
