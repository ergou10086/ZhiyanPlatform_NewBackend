package hbnu.project.zhiyanbackend.security.encrypt.filter;

import cn.hutool.core.io.IoUtil;
import hbnu.project.zhiyanbackend.basic.constants.GeneralConstants;
import hbnu.project.zhiyanbackend.security.encrypt.utils.EncryptUtils;
import hbnu.project.zhiyanbackend.security.encrypt.utils.FieldEncryptUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 解密请求参数工具类
 * 支持全量加密和字段级别加密两种模式
 *
 * @author wdhcr
 * @rewrite ErgouTree
 * @version 3.0.0
 */
public class DecryptRequestBodyWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    /**
     * 全量加密模式构造方法
     */
    public DecryptRequestBodyWrapper(HttpServletRequest request, String privateKey, String headerFlag,
                                     FieldEncryptUtils fieldEncryptUtils, Class<?> requestBodyClass) throws IOException {
        super(request);
        request.setCharacterEncoding(GeneralConstants.UTF8);
        byte[] readBytes = IoUtil.readBytes(request.getInputStream(), false);
        String requestBody = new String(readBytes, StandardCharsets.UTF_8);
        
        String decryptBody;
        if (fieldEncryptUtils != null && requestBodyClass != null) {
            // 字段加密模式：先解密整个body，再解密字段
            String headerRsa = request.getHeader(headerFlag);
            if (StringUtils.isNotBlank(headerRsa)) {
                String decryptAes = EncryptUtils.decryptByRsa(headerRsa, privateKey);
                String aesPassword = EncryptUtils.decryptByBase64(decryptAes);
                String fullDecryptBody = EncryptUtils.decryptByAes(requestBody, aesPassword);
                // 对字段进行解密
                decryptBody = fieldEncryptUtils.decryptRequestFields(fullDecryptBody, requestBodyClass);
            } else {
                // 没有加密标头，可能是字段直接加密的情况
                decryptBody = fieldEncryptUtils.decryptRequestFields(requestBody, requestBodyClass);
            }
        } else {
            // 全量加密模式
            String headerRsa = request.getHeader(headerFlag);
            String decryptAes = EncryptUtils.decryptByRsa(headerRsa, privateKey);
            String aesPassword = EncryptUtils.decryptByBase64(decryptAes);
            decryptBody = EncryptUtils.decryptByAes(requestBody, aesPassword);
        }
        
        body = decryptBody.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }


    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public String getContentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }


    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public int available() {
                return body.length;
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
