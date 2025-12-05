package hbnu.project.zhiyanbackend.auth.utils;

import lombok.extern.slf4j.Slf4j;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;
import static java.util.Base64.getEncoder;

/**
 * 双因素认证工具类
 * 基于TOTP（Time-based One-Time Password）算法
 * 兼容 Microsoft Authenticator、Google Authenticator 等
 *
 * @author ErgouTree
 */
@Slf4j
public class TwoFactorAuthUtil {

    /**
     * 哈希算法
     */
    private static final String ALGORITHM = "HmacSHA1";

    /**
     * TOTP时间步长，秒
     */
    private static final int TIME_STEP = 30;

    /**
     * 验证码位数
     */
    private static final int CODE_DIGITS = 6;

    /**
     * 发行名称
     */
    private static final String ISSUER = "智研平台";


    /**
     * 生成随机密钥（Base32编码）
     *
     * @return Base32编码的密钥字符串
     */
    public static String generateSecretKey() {
        // 生成20字节的随机密钥（160位）
        byte[] keyBytes = new byte[20];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(keyBytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(keyBytes);
    }

    /**
     * 生成二维码内容（OTPAUTH URL）
     *
     * @param secretKey 密钥（Base32编码）
     * @param account   账号标识（通常是邮箱）
     * @param issuer    发行者名称（可选）
     * @return OTPAUTH URL字符串
     */
    public static String getQrCodeText(String secretKey, String account, String issuer) {
        String normalizedBase32Key = secretKey.replace(" ", "").toUpperCase();

        try{
            String issuerName = StringUtils.isNotBlank(issuer) ? issuer : ISSUER;
            String url = "otpauth://totp/"
                    + URLEncoder.encode(issuerName + ":" + account, StandardCharsets.UTF_8).replace("+", "%20")
                    + "?secret=" + URLEncoder.encode(normalizedBase32Key, StandardCharsets.UTF_8).replace("+", "%20")
                    + "&issuer=" + URLEncoder.encode(issuerName, StandardCharsets.UTF_8).replace("+", "%20")
                    + "&algorithm=SHA1"
                    + "&digits=" + CODE_DIGITS
                    + "&period=" + TIME_STEP;

            return url;
        }catch (Exception e){
            log.error("生成二维码内容失败", e);
            throw new IllegalStateException("生成二维码内容失败", e);
        }
    }

    /**
     * 生成二维码图片（Base64编码）
     *
     * @param qrCodeText 二维码内容
     * @param width      图片宽度
     * @param height     图片高度
     * @return Base64编码的图片字符串
     */
    public static String getQrCodeBase64(String qrCodeText, int width, int height) {
        try{
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] imageData = outputStream.toByteArray();
            return getEncoder().encodeToString(imageData);
        }catch (Exception e){
            log.error("生成二维码图片失败或者生成二维码Base64失败", e);
            throw new IllegalStateException("生成二维码图片失败或生成二维码Base64失败", e);
        }
    }

    /**
     * 验证TOTP验证码
     *
     * @param secretKey 密钥（Base32编码）
     * @param code      用户输入的验证码
     * @return 验证是否通过
     */
    public static boolean verifyCode(String secretKey, String code) {
        if (StringUtils.isBlank(secretKey) || StringUtils.isBlank(code)) {
            return false;
        }

        try {
            // 允许时间窗口：当前时间步长 ±1（容错1个时间窗口，即±30秒）
            long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP;

            // 验证当前时间步长和前后各一个时间步长
            for (int i = -1; i <= 1; i++) {
                long timeStep = currentTimeStep + i;
                String expectedCode = generateTOTP(secretKey, timeStep);
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("验证TOTP验证码失败", e);
            return false;
        }
    }

    /**
     * 生成TOTP验证码
     *
     * @param secretKey 密钥（Base32编码）
     * @param timeStep  时间步长
     * @return 6位数字验证码
     */
    private static String generateTOTP(String secretKey, long timeStep) {
        try{
            // 解码Base32密钥
            Base32 base32 = new Base32();
            byte[] key = base32.decode(secretKey.toUpperCase());

            // 将时间步长转换为8字节数组（大端序）
            byte[] time = new byte[8];
            for (int i = 7; i >= 0; i--) {
                time[i] = (byte) (timeStep & 0xFF);
                timeStep >>= 8;
            }

            // 使用HMAC-SHA1计算哈希
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(time);

            // 动态截取（RFC 4226）
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            // 生成6位数字验证码
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        }catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成TOTP验证码失败", e);
            throw new IllegalStateException("生成TOTP验证码失败", e);
        }
    }
}
