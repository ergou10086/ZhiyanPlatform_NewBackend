package hbnu.project.zhiyanbackend.security.encrypt.filter;

import cn.hutool.core.util.ObjectUtil;
import hbnu.project.zhiyanbackend.basic.exception.ServiceException;
import hbnu.project.zhiyanbackend.basic.utils.SpringUtils;
import hbnu.project.zhiyanbackend.security.encrypt.annotation.ApiEncrypt;
import hbnu.project.zhiyanbackend.security.encrypt.config.properties.ApiDecryptProperties;
import hbnu.project.zhiyanbackend.security.encrypt.config.properties.EncryptorProperties;
import hbnu.project.zhiyanbackend.security.encrypt.core.EncryptorManager;
import hbnu.project.zhiyanbackend.security.encrypt.utils.FieldEncryptUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.lang.reflect.Parameter;


/**
 * Crypto 过滤器
 * 支持全量加密和字段级别加密两种模式
 *
 * @author wdhcr
 * @rewrite ErgouTree
 * @version 3.0.0
 */
public class CryptoFilter implements Filter {
    private final ApiDecryptProperties properties;
    private final EncryptorManager encryptorManager;
    private final EncryptorProperties encryptorProperties;
    private final FieldEncryptUtils fieldEncryptUtils;

    public CryptoFilter(ApiDecryptProperties properties, 
                       EncryptorManager encryptorManager,
                       EncryptorProperties encryptorProperties,
                       FieldEncryptUtils fieldEncryptUtils) {
        this.properties = properties;
        this.encryptorManager = encryptorManager;
        this.encryptorProperties = encryptorProperties;
        this.fieldEncryptUtils = fieldEncryptUtils;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        
        // 获取加密注解和HandlerMethod
        HandlerMethodInfo handlerInfo = this.getHandlerMethodInfo(servletRequest);
        ApiEncrypt apiEncrypt = handlerInfo.getApiEncrypt();
        HandlerMethod handlerMethod = handlerInfo.getHandlerMethod();
        
        if (apiEncrypt == null) {
            // 没有加密注解，直接放行
            chain.doFilter(request, response);
            return;
        }

        // 判断加密模式
        ApiEncrypt.EncryptMode mode = apiEncrypt.mode();
        boolean requestFlag = apiEncrypt.request() || apiEncrypt.requestEncrypt();
        boolean responseFlag = apiEncrypt.response() || apiEncrypt.responseEncrypt();
        
        ServletRequest requestWrapper = null;
        ServletResponse responseWrapper = null;
        EncryptResponseBodyWrapper responseBodyWrapper = null;

        // 处理请求解密
        if (requestFlag && (HttpMethod.PUT.matches(servletRequest.getMethod()) || 
                           HttpMethod.POST.matches(servletRequest.getMethod()))) {
            if (mode == ApiEncrypt.EncryptMode.FULL) {
                // 全量加密模式
                String headerValue = servletRequest.getHeader(properties.getHeaderFlag());
                if (StringUtils.isNotBlank(headerValue)) {
                    requestWrapper = new DecryptRequestBodyWrapper(
                        servletRequest, properties.getPrivateKey(), properties.getHeaderFlag(), 
                        null, null);
                } else {
                    // 需要加密但没有加密标头，拒绝访问
                    HandlerExceptionResolver exceptionResolver = SpringUtils.getBean("handlerExceptionResolver", HandlerExceptionResolver.class);
                    exceptionResolver.resolveException(
                        servletRequest, servletResponse, null,
                        new ServiceException("没有访问权限，请联系管理员授权", HttpStatus.FORBIDDEN));
                    return;
                }
            } else {
                // 字段加密模式
                Class<?> requestBodyClass = getRequestBodyClass(handlerMethod);
                requestWrapper = new DecryptRequestBodyWrapper(
                    servletRequest, properties.getPrivateKey(), properties.getHeaderFlag(),
                    fieldEncryptUtils, requestBodyClass);
            }
        }

        // 处理响应加密
        if (responseFlag) {
            Class<?> responseBodyClass = getResponseBodyClass(handlerMethod);
            responseBodyWrapper = new EncryptResponseBodyWrapper(
                servletResponse, mode, fieldEncryptUtils, responseBodyClass);
            responseWrapper = responseBodyWrapper;
        }

        chain.doFilter(
            ObjectUtil.defaultIfNull(requestWrapper, request),
            ObjectUtil.defaultIfNull(responseWrapper, response));

        // 处理响应加密
        if (responseFlag && responseBodyWrapper != null) {
            servletResponse.reset();
            String encryptContent;
            if (mode == ApiEncrypt.EncryptMode.FULL) {
                // 全量加密
                encryptContent = responseBodyWrapper.getEncryptContent(
                    servletResponse, properties.getPublicKey(), properties.getHeaderFlag());
            } else {
                // 字段加密
                encryptContent = responseBodyWrapper.getFieldEncryptContent(
                    servletResponse, properties.getPublicKey(), properties.getHeaderFlag());
            }
            servletResponse.getWriter().write(encryptContent);
        }
    }

    /**
     * 获取HandlerMethod信息
     */
    private HandlerMethodInfo getHandlerMethodInfo(HttpServletRequest servletRequest) {
        RequestMappingHandlerMapping handlerMapping = SpringUtils.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        try {
            HandlerExecutionChain mappingHandler = handlerMapping.getHandler(servletRequest);
            if (ObjectUtil.isNotNull(mappingHandler)) {
                Object handler = mappingHandler.getHandler();
                if (ObjectUtil.isNotNull(handler) && handler instanceof HandlerMethod handlerMethod) {
                    ApiEncrypt apiEncrypt = handlerMethod.getMethodAnnotation(ApiEncrypt.class);
                    return new HandlerMethodInfo(apiEncrypt, handlerMethod);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return new HandlerMethodInfo(null, null);
    }

    /**
     * 获取请求体类型
     */
    private Class<?> getRequestBodyClass(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }
        Parameter[] parameters = handlerMethod.getMethod().getParameters();
        for (Parameter parameter : parameters) {
            // 检查是否有@RequestBody注解
            if (parameter.isAnnotationPresent(org.springframework.web.bind.annotation.RequestBody.class)) {
                return parameter.getType();
            }
        }
        return null;
    }

    /**
     * 获取响应体类型
     */
    private Class<?> getResponseBodyClass(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }
        // 尝试从返回类型获取
        Class<?> returnType = handlerMethod.getMethod().getReturnType();
        // 如果是ResponseEntity，尝试获取泛型类型
        if (org.springframework.http.ResponseEntity.class.isAssignableFrom(returnType)) {
            // 简化处理，返回Object.class，实际使用时需要更复杂的泛型解析
            return Object.class;
        }
        return returnType;
    }

    /**
     * HandlerMethod信息封装类
     */
    private static class HandlerMethodInfo {
        private final ApiEncrypt apiEncrypt;
        private final HandlerMethod handlerMethod;

        public HandlerMethodInfo(ApiEncrypt apiEncrypt, HandlerMethod handlerMethod) {
            this.apiEncrypt = apiEncrypt;
            this.handlerMethod = handlerMethod;
        }

        public ApiEncrypt getApiEncrypt() {
            return apiEncrypt;
        }

        public HandlerMethod getHandlerMethod() {
            return handlerMethod;
        }
    }

    @Override
    public void destroy() {
    }
}
