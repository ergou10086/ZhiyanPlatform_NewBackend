package hbnu.project.zhiyanbackend.auth.controller;

import hbnu.project.zhiyanbackend.auth.model.dto.QRCodeLoginDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserLoginResponseDTO;
import hbnu.project.zhiyanbackend.auth.service.QRCodeLoginService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 扫码登录控制器
 * 支持移动端扫码授权PC端登录
 *
 * @author ErgouTree
 */
@Slf4j
@RestController
@RequestMapping("/zhiyan/auth/qrcode")
@RequiredArgsConstructor
@Tag(name = "扫码登录", description = "移动端扫码授权PC端登录")
public class QRCodeLoginController {

    private final QRCodeLoginService qrCodeLoginService;

    /**
     * 生成登录二维码
     * PC端调用,获取二维码用于移动端扫描
     */
    @PostMapping("/generate")
    @Operation(summary = "生成登录二维码", description = "PC端生成二维码供移动端扫描")
    public R<QRCodeLoginDTO> generateQRCode() {
        log.info("PC端请求生成登录二维码");
        return qrCodeLoginService.generateQRCode();
    }

    /**
     * 查询二维码状态
     * 检查二维码是否被扫描/授权/过期等
     */
    @GetMapping("/status/{qrCodeId}")
    @Operation(summary = "查询二维码状态", description = "PC端轮询二维码状态")
    public R<QRCodeLoginDTO> checkQRCodeStatus(@PathVariable String qrCodeId) {
        log.debug("PC端轮询二维码状态: qrCodeId={}", qrCodeId);
        return qrCodeLoginService.getQRCodeStatus(qrCodeId);
    }

    /**
     * 移动端扫描二维码
     * 移动端扫描后调用此接口,标记二维码已被扫描
     */
    @PostMapping("/scan/{qrCodeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "扫描二维码", description = "移动端扫描二维码")
    public R<Void> scanQRCode(@PathVariable String qrCodeId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.info("移动端扫描二维码: userId={}, qrCodeId={}", userId, qrCodeId);
        return qrCodeLoginService.scanQRCode(qrCodeId, userId);
    }

    /**
     * 移动端确认登录
     * 移动端确认授权后调用该方法确认登录
     */
    @PostMapping("/confirm/{qrCodeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "确认登录", description = "移动端确认授权PC端登录")
    public R<Void> confirmLogin(@PathVariable String qrCodeId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }
        log.info("移动端确认登录: userId={}, qrCodeId={}", userId, qrCodeId);
        return qrCodeLoginService.confirmLogin(qrCodeId, userId);
    }

    /**
     * 移动端取消登录
     * 移动端取消授权
     */
    @PostMapping("/cancel/{qrCodeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "取消登录", description = "移动端取消授权")
    public R<Void> cancelLogin(@PathVariable String qrCodeId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return R.fail(R.UNAUTHORIZED, "未登录或令牌无效");
        }

        log.info("移动端取消登录: userId={}, qrCodeId={}", userId, qrCodeId);
        return qrCodeLoginService.cancelLogin(qrCodeId, userId);
    }

    /**
     * PC端获取登录的结果
     * 在二维码被扫描后，PC端调用此接口获取登录Token
     */
    @GetMapping("/result/{qrCodeId}")
    @Operation(summary = "获得登录结果", description = "PC端获取登录Token")
    public R<UserLoginResponseDTO> getLoginResult(@PathVariable String qrCodeId) {
        log.info("PC端获取登录结果: qrCodeId={}", qrCodeId);
        return qrCodeLoginService.getLoginResult(qrCodeId);
    }
}
