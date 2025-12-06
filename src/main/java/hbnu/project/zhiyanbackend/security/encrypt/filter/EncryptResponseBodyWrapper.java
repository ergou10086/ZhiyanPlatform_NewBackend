package hbnu.project.zhiyanbackend.security.encrypt.filter;

import hbnu.project.zhiyanbackend.security.encrypt.annotation.ApiEncrypt;
import hbnu.project.zhiyanbackend.security.encrypt.utils.EncryptUtils;
import hbnu.project.zhiyanbackend.security.encrypt.utils.FieldEncryptUtils;
import cn.hutool.core.util.RandomUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 加密响应参数包装类
 * 支持全量加密和字段级别加密两种模式
 *
 * @author Michelle.Chung
 * @rewrite ErgouTree
 * @version 3.0.0
 */
public class EncryptResponseBodyWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream byteArrayOutputStream;
    private final ServletOutputStream servletOutputStream;
    private final PrintWriter printWriter;
    private final ApiEncrypt.EncryptMode mode;
    private final FieldEncryptUtils fieldEncryptUtils;
    private final Class<?> responseBodyClass;

    /**
     * 构造方法
     *
     * @param response 响应对象
     * @param mode 加密模式
     * @param fieldEncryptUtils 字段加密工具类
     * @param responseBodyClass 响应体类型
     */
    public EncryptResponseBodyWrapper(HttpServletResponse response, 
                                     ApiEncrypt.EncryptMode mode,
                                     FieldEncryptUtils fieldEncryptUtils,
                                     Class<?> responseBodyClass) throws IOException {
        super(response);
        this.mode = mode;
        this.fieldEncryptUtils = fieldEncryptUtils;
        this.responseBodyClass = responseBodyClass;
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.servletOutputStream = this.getOutputStream();
        this.printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream));
    }

    @Override
    public PrintWriter getWriter() {
        return printWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (servletOutputStream != null) {
            servletOutputStream.flush();
        }
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    @Override
    public void reset() {
        byteArrayOutputStream.reset();
    }

    public byte[] getResponseData() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toByteArray();
    }

    public String getContent() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toString();
    }

    /**
     * 获取全量加密内容
     *
     * @param servletResponse response
     * @param publicKey       RSA公钥 (用于加密 AES 秘钥)
     * @param headerFlag      请求头标志
     * @return 加密内容
     */
    public String getEncryptContent(HttpServletResponse servletResponse, String publicKey, String headerFlag) throws IOException {
        // 生成秘钥
        String aesPassword = RandomUtil.randomString(32);
        // 秘钥使用 Base64 编码
        String encryptAes = EncryptUtils.encryptByBase64(aesPassword);
        // Rsa 公钥加密 Base64 编码
        String encryptPassword = EncryptUtils.encryptByRsa(encryptAes, publicKey);

        // 设置响应头
        servletResponse.setHeader(headerFlag, encryptPassword);
        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.toString());

        // 获取原始内容
        String originalBody = this.getContent();
        // 对内容进行加密
        return EncryptUtils.encryptByAes(originalBody, aesPassword);
    }

    /**
     * 获取字段加密内容
     *
     * @param servletResponse response
     * @param publicKey       RSA公钥 (用于加密 AES 秘钥)
     * @param headerFlag      请求头标志
     * @return 加密内容
     */
    public String getFieldEncryptContent(HttpServletResponse servletResponse, String publicKey, String headerFlag) throws IOException {
        // 获取原始内容
        String originalBody = this.getContent();
        
        // 对字段进行加密
        String fieldEncryptedBody = originalBody;
        if (fieldEncryptUtils != null && responseBodyClass != null) {
            fieldEncryptedBody = fieldEncryptUtils.encryptResponseFields(originalBody, responseBodyClass);
        }
        
        // 生成秘钥
        String aesPassword = RandomUtil.randomString(32);
        // 秘钥使用 Base64 编码
        String encryptAes = EncryptUtils.encryptByBase64(aesPassword);
        // Rsa 公钥加密 Base64 编码
        String encryptPassword = EncryptUtils.encryptByRsa(encryptAes, publicKey);

        // 设置响应头
        servletResponse.setHeader(headerFlag, encryptPassword);
        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.toString());

        // 对整个body进行加密（字段已经加密，这里是对整个JSON进行二次加密）
        return EncryptUtils.encryptByAes(fieldEncryptedBody, aesPassword);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byteArrayOutputStream.write(b, off, len);
            }
        };
    }

}
