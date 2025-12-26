package hbnu.project.zhiyanbackend.auth.service.impl;

import cn.hutool.core.lang.UUID;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import hbnu.project.zhiyanbackend.auth.model.dto.QRCodeLoginDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.TokenDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserDTO;
import hbnu.project.zhiyanbackend.auth.model.dto.UserLoginResponseDTO;
import hbnu.project.zhiyanbackend.auth.model.entity.QRCodeLogin;
import hbnu.project.zhiyanbackend.auth.model.entity.User;
import hbnu.project.zhiyanbackend.auth.model.enums.QRCodeStatus;
import hbnu.project.zhiyanbackend.auth.repository.QRCodeLoginRepository;
import hbnu.project.zhiyanbackend.auth.repository.UserRepository;
import hbnu.project.zhiyanbackend.auth.service.AuthService;
import hbnu.project.zhiyanbackend.auth.service.QRCodeLoginService;
import hbnu.project.zhiyanbackend.basic.domain.R;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.exception.UtilException;
import hbnu.project.zhiyanbackend.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 扫码登录服务实现
 *
 * @author ErgouTree
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeLoginServiceImpl implements QRCodeLoginService {

    private final QRCodeLoginRepository qrCodeLoginRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final RedisService redisService;
    private final AuthUserDetailsServiceImpl authUserDetailsService;

    // 二维码有效期(分钟)
    private static final int QR_CODE_EXPIRE_MINUTES = 5;

    // 二维码图片尺寸
    private static final int QR_CODE_SIZE = 300;

    // Redis Key前缀
    private static final String QR_CODE_CACHE_PREFIX = "qrcode:login:";

    /**
     * 生成登录二维码
     *
     * @return 二维码信息，包含二维码ID和过期时间
     */
    @Override
    @Transactional
    public R<QRCodeLoginDTO> generateQRCode() {
        try{
            // 1. 生成唯一的二维码ID
            String qrCodeId = UUID.randomUUID().toString().replace("-", "");

            // 2.计算过期时间
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(QR_CODE_EXPIRE_MINUTES);

            // 3.保存到数据库
            QRCodeLogin qrCodeLogin = QRCodeLogin.builder()
                    .qrCode(qrCodeId)
                    .status(QRCodeStatus.PENDING)
                    .expireTime(expireTime)
                    .build();
            qrCodeLoginRepository.save(qrCodeLogin);

            // 4.缓存到Redis
            String cacheKey = QR_CODE_CACHE_PREFIX + qrCodeId;
            redisService.setCacheObject(cacheKey, QRCodeStatus.PENDING.name(), (long)QR_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            // 5.生成二维码内容
            String qrCodeContent = "zhiyan://qrlogin?code=" + qrCodeId;

            // 6. 生成二维码图片(Base64)
            String qrCodeBase64 = generateQRCodeImage(qrCodeContent);

            // 7. 计算剩余时间
            long remainingSeconds = Duration.between(LocalDateTime.now(), expireTime).getSeconds();

            // 8. 构建响应
            QRCodeLoginDTO response = QRCodeLoginDTO.builder()
                    .qrCodeId(qrCodeId)
                    .qrCodeContent(qrCodeContent)
                    .qrCodeBase64(qrCodeBase64)
                    .status(QRCodeStatus.PENDING)
                    .expireTime(expireTime)
                    .remainingSeconds(remainingSeconds)
                    .build();

            log.info("生成登录二维码成功: qrCodeId={}, expireTime={}", qrCodeId, expireTime);
            return R.ok(response, "二维码生成成功");
        }catch (ServiceException e){
            log.error("生成登录二维码失败", e);
            return R.fail("生成二维码失败,请稍后重试");
        }
    }

    /**
     * 获取二维码状态
     *
     * @param qrCodeId 二维码id
     * @return 二维码状态信息
     */
    @Override
    public R<QRCodeLoginDTO> getQRCodeStatus(String qrCodeId) {
        try {
            // 1. 先从Redis查询状态(快速响应)
            String cacheKey = QR_CODE_CACHE_PREFIX + qrCodeId;
            String statusStr = redisService.getCacheObject(cacheKey);

            // 2. 从数据库查询完整信息
            Optional<QRCodeLogin> qrCodeLoginOpt = qrCodeLoginRepository.findByQrCode(qrCodeId);
            if(qrCodeLoginOpt.isEmpty()){
                return R.fail("二维码不存在或已过期");
            }
            QRCodeLogin qrCodeLogin = qrCodeLoginOpt.get();

            // 3. 检查是否过期
            if (qrCodeLogin.isExpired() && qrCodeLogin.getStatus() != QRCodeStatus.EXPIRED) {
                qrCodeLogin.setStatus(QRCodeStatus.EXPIRED);
                qrCodeLoginRepository.save(qrCodeLogin);
                redisService.deleteObject(cacheKey);
            }

            // 4.构建响应
            QRCodeLoginDTO response = buildQRCodeDTO(qrCodeLogin);

            return R.ok(response);
        }catch (ServiceException e){
            log.error("查询二维码状态失败: qrCodeId={}", qrCodeId, e);
            return R.fail("查询失败");
        }
    }

    /**
     * 移动端扫描二维码
     *
     * @param qrCodeId 二维码ID
     * @param userId   扫描用户ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> scanQRCode(String qrCodeId, Long userId) {
        try{
            // 1.查询二维码
            Optional<QRCodeLogin> qrCodeLoginOpt = qrCodeLoginRepository.findByQrCode(qrCodeId);
            if(qrCodeLoginOpt.isEmpty()){
                return R.fail("二维码不存在或已过期");
            }
            QRCodeLogin qrCodeLogin = qrCodeLoginOpt.get();

            // 2.检查是否可以扫描
            if(!qrCodeLogin.canBeScanned()){
                if(qrCodeLogin.isExpired()) {
                    return R.fail("二维码已过期");
                }
                return R.fail("二维码已过期");
            }

            // 3. 更新状态为已扫描
            qrCodeLogin.setStatus(QRCodeStatus.SCANNED);
            qrCodeLogin.setScanUserId(userId);
            qrCodeLogin.setScanTime(LocalDateTime.now());
            qrCodeLoginRepository.save(qrCodeLogin);

            // 4. 更新Redis缓存
            String cacheKey = QR_CODE_CACHE_PREFIX + qrCodeId;
            redisService.setCacheObject(cacheKey, QRCodeStatus.SCANNED.name(), (long)QR_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            log.info("用户扫描二维码成功: userId={}, qrCodeId={}", userId, qrCodeId);
            return R.ok(null, "扫描成功");
        }catch (ServiceException e){
            log.error("扫描二维码失败: qrCodeId={}, userId={}", qrCodeId, userId, e);
            return R.fail("扫描失败,请重试");
        }
    }

    /**
     * 移动端确认登录
     *
     * @param qrCodeId 二维码ID
     * @param userId   确认用户ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> confirmLogin(String qrCodeId, Long userId) {
        try {
            // 1. 查询二维码
            Optional<QRCodeLogin> qrCodeOpt = qrCodeLoginRepository.findByQrCode(qrCodeId);
            if (qrCodeOpt.isEmpty()) {
                return R.fail("二维码不存在或已过期");
            }
            QRCodeLogin qrCodeLogin = qrCodeOpt.get();

            // 2.验证是否是扫描用户
            if(!userId.equals(qrCodeLogin.getScanUserId())){
                return R.fail("无权操作此二维码");
            }

            // 3.检查该二维码是否可以扫
            if(!qrCodeLogin.canBeConfirmed()){
                if(qrCodeLogin.isExpired()) {
                    return R.fail("二维码已过期");
                }
                return R.fail("二维码状态异常,无法确认");
            }

            // 4. 生成JWT Token(不使用RememberMe)
            TokenDTO tokenDTO = authService.generateTokens(userId, false);

            // 5.更新二维码状态，并且存入Token的相关信息
            qrCodeLogin.setStatus(QRCodeStatus.CONFIRMED);
            qrCodeLogin.setConfirmTime(LocalDateTime.now());
            qrCodeLogin.setAccessToken(tokenDTO.getAccessToken());
            qrCodeLogin.setRefreshToken(tokenDTO.getRefreshToken());
            qrCodeLogin.setTokenExpiresIn(tokenDTO.getExpiresIn());
            qrCodeLoginRepository.save(qrCodeLogin);

            // 6.更新Redis缓存
            String cacheKey = QR_CODE_CACHE_PREFIX + qrCodeId;
            // 稍微长点，怕PC那边拿不到
            redisService.setCacheObject(cacheKey, QRCodeStatus.CONFIRMED.name(), (long)10, TimeUnit.MINUTES);

            log.info("用户确认登录成功: userId={}, qrCodeId={}", userId, qrCodeId);
            return R.ok(null, "授权成功");
        }catch (ServiceException e){
            log.error("确认登录失败: qrCodeId={}, userId={}", qrCodeId, userId, e);
            return R.fail("确认失败,请重试");
        }
    }

    /**
     * 移动端取消登录
     *
     * @param qrCodeId 二维码ID
     * @param userId   操作用户的id
     * @return 操作结果
     */
    @Override
    @Transactional
    public R<Void> cancelLogin(String qrCodeId, Long userId) {
        try{
            // 1. 查询二维码
            Optional<QRCodeLogin> qrCodeOpt = qrCodeLoginRepository.findByQrCode(qrCodeId);
            if (qrCodeOpt.isEmpty()) {
                return R.fail("二维码不存在或已过期");
            }
            QRCodeLogin qrCodeLogin = qrCodeOpt.get();

            // 2. 验证是否是扫描用户
            if(!userId.equals(qrCodeOpt.get().getScanUserId())){
                return R.fail("无权操作此二维码");
            }

            // 3.更新二维码状态
            qrCodeLogin.setStatus(QRCodeStatus.CANCELLED);
            qrCodeLoginRepository.save(qrCodeLogin);

            // 4. 更新Redis缓存
            String cacheKey = QR_CODE_CACHE_PREFIX + qrCodeId;
            redisService.setCacheObject(cacheKey, QRCodeStatus.CANCELLED.name(), (long)1, TimeUnit.MINUTES);

            log.info("用户取消登录: userId={}, qrCodeId={}", userId, qrCodeId);
            return R.ok(null, "已取消");
        }catch (ServiceException e){
            log.error("取消登录失败: qrCodeId={}, userId={}", qrCodeId, userId, e);
            return R.fail("取消失败,请重试");
        }
    }

    /**
     * PC端获取登录结果
     *
     * @param qrCodeId 二维码ID
     * @return 登录结果(包含Token)
     */
    @Override
    public R<UserLoginResponseDTO> getLoginResult(String qrCodeId) {
        try{
            // 1.查询二维码
            Optional<QRCodeLogin> qrCodeLoginOpt = qrCodeLoginRepository.findByQrCode(qrCodeId);
            if (qrCodeLoginOpt.isEmpty()) {
                return R.fail("二维码不存在或已过期");
            }
            QRCodeLogin qrCodeLogin = qrCodeLoginOpt.get();

            // 2.检查二维码状态
            if (qrCodeLogin.getStatus() != QRCodeStatus.CONFIRMED) {
                return R.fail("登录尚未确认");
            }

            // 3.获取用户的信息
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(qrCodeLogin.getScanUserId());
            if (userOpt.isEmpty()) {
                return R.fail("用户不存在");
            }
            User user = userOpt.get();

            // 4.构建用户DTO
            UserDTO userDTO = buildUserDTO(user);

            // 5.构建响应
            UserLoginResponseDTO response = UserLoginResponseDTO.builder()
                    .user(userDTO)
                    .accessToken(qrCodeLogin.getAccessToken())
                    .refreshToken(qrCodeLogin.getRefreshToken())
                    .expiresIn(qrCodeLogin.getTokenExpiresIn())
                    .tokenType("Bearer")
                    .rememberMe(false)
                    .build();

            log.info("PC端获取登录结果成功: qrCodeId={}, userId={}", qrCodeId, qrCodeLogin.getScanUserId());

            return R.ok(response, "登录成功");
        }catch (ServiceException e){
            log.error("获取登录结果失败: qrCodeId={}", qrCodeId, e);
            return R.fail("获取登录信息失败");
        }
    }

    /**
     * 清理过期的二维码
     */
    @Override
    @Transactional
    public void cleanExpiredQRCodes() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = qrCodeLoginRepository.deleteByExpireTimeBefore(now);
            if (deletedCount > 0) {
                log.info("清理了 {} 个过期的二维码", deletedCount);
            }
        } catch (Exception e) {
            log.error("清理过期二维码失败", e);
        }
    }

    // ------------------------- 其他辅助方法 -----------------------------

    /**
     * 生成二维码图片(Base64编码)
     */
    private String generateQRCodeImage(String content) {
        try{
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content,  BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrCodeBytes = outputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeBytes);
        }catch (UtilException | IOException | WriterException e){
            log.error("生成二维码图片失败", e);
            throw new RuntimeException("生成二维码图片失败");
        }
    }

    /**
     * 构建QRCodeLoginDTO
     */
    private QRCodeLoginDTO buildQRCodeDTO(QRCodeLogin qrCodeLogin) {
        QRCodeLoginDTO dto = QRCodeLoginDTO.builder()
                .qrCodeId(qrCodeLogin.getQrCode())
                .status(qrCodeLogin.getStatus())
                .expireTime(qrCodeLogin.getExpireTime())
                .build();

        // 计算剩余时间
        if (!qrCodeLogin.isExpired()) {
            long remainingSeconds = Duration.between(
                    LocalDateTime.now(),
                    qrCodeLogin.getExpireTime()
            ).getSeconds();
            dto.setRemainingSeconds(Math.max(0, remainingSeconds));
        } else {
            dto.setRemainingSeconds(0L);
        }

        // 如果已扫描,添加扫描用户信息
        if (qrCodeLogin.getScanUserId() != null) {
            Optional<User> userOpt = userRepository.findByIdAndIsDeletedFalse(
                    qrCodeLogin.getScanUserId());
            userOpt.ifPresent(user -> {
                QRCodeLoginDTO.ScanUserInfo scanUser = QRCodeLoginDTO.ScanUserInfo.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .email(maskEmail(user.getEmail()))
                        .build();

                // 添加头像信息
                if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
                    try {
                        String base64 = Base64.getEncoder().encodeToString(user.getAvatarData());
                        String contentType = user.getAvatarContentType() != null ?
                                user.getAvatarContentType() : "image/jpeg";
                        scanUser.setAvatarData("data:" + contentType + ";base64," + base64);
                    } catch (Exception e) {
                        log.warn("处理头像数据失败: userId={}", user.getId(), e);
                    }
                }

                dto.setScanUser(scanUser);
            });
        }

        return dto;
    }

    /**
     * 构建UserDTO
     */
    private UserDTO buildUserDTO(User user) {
        // 处理头像
        String avatarData = null;
        if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
            try {
                String base64 = Base64.getEncoder().encodeToString(user.getAvatarData());
                String contentType = user.getAvatarContentType() != null ?
                        user.getAvatarContentType() : "image/jpeg";
                avatarData = "data:" + contentType + ";base64," + base64;
            } catch (Exception e) {
                log.warn("处理头像数据失败: userId={}", user.getId(), e);
            }
        }

        // 获取用户角色
        List<String> roles = authUserDetailsService.getUserRoles(user.getId());

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarData(avatarData)
                .avatarContentType(user.getAvatarContentType())
                .title(user.getTitle())
                .institution(user.getInstitution())
                .roles(roles)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .build();
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        if (username.length() <= 2) {
            return username.charAt(0) + "***@" + parts[1];
        }
        return username.substring(0, 2) + "***@" + parts[1];
    }
}
