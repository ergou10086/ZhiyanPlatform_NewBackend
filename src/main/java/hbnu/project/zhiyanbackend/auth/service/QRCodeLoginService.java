package hbnu.project.zhiyanbackend.auth.service;

import hbnu.project.zhiyanbackend.auth.model.dto.QRCodeLoginDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserLoginResponseDTO;
import hbnu.project.zhiyanbackend.basic.domain.R;

/**
 * 扫码登录服务实现
 *
 * @author ErgouTree
 */
public interface QRCodeLoginService {

    /**
     * 生成登录二维码
     *
     * @return 二维码信息，包含二维码ID和过期时间
     */
    R<QRCodeLoginDTO> generateQRCode();

    /**
     * 获取二维码状态
     *
     * @param qrCodeId 二维码id
     * @return 二维码状态信息
     */
    R<QRCodeLoginDTO> getQRCodeStatus(String qrCodeId);

    /**
     * 移动端扫描二维码
     *
     * @param qrCodeId 二维码ID
     * @param userId   扫描用户ID
     * @return 操作结果
     */
    R<Void> scanQRCode(String qrCodeId, Long userId);

    /**
     * 移动端确认登录
     *
     * @param qrCodeId 二维码ID
     * @param userId   确认用户ID
     * @return 操作结果
     */
    R<Void> confirmLogin(String qrCodeId, Long userId);

    /**
     * 移动端取消登录
     *
     * @param qrCodeId 二维码ID
     * @param userId  操作用户的id
     * @return 操作结果
     */
    R<Void> cancelLogin(String qrCodeId, Long userId);

    /**
     * PC端获取登录结果
     *
     * @param qrCodeId 二维码ID
     * @return 登录结果(包含Token)
     */
    R<UserLoginResponseDTO> getLoginResult(String qrCodeId);

    /**
     * 清理过期的二维码
     */
    void cleanExpiredQRCodes();
}
