package hbnu.project.zhiyanbackend.activelog.aspect;

import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;
import hbnu.project.zhiyanbackend.activelog.core.OperationLogContext;
import hbnu.project.zhiyanbackend.activelog.core.OperationLogSaveCore;
import hbnu.project.zhiyanbackend.basic.utils.JsonUtils;
import hbnu.project.zhiyanbackend.security.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 * 使用AOP自动记录操作日志，采用异步处理避免影响业务性能
 * 简化版：去除SpEL表达式，使用ThreadLocal上下文传递参数
 *
 * @author ErgouTree
 */
@Slf4j
@Aspect
@Component("activelogOperationLogAspect")
@Order(2)
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogSaveCore operationLogSaveCore;

    /**
     * 定义切点：所有带有@BizOperationLog注解的方法
     */
    @Pointcut("@annotation(hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知：拦截操作并记录日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 签名中获取注解
        BizOperationLog annotation = method.getAnnotation(BizOperationLog.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 获取用户信息
        Long userId = SecurityUtils.getUserId();
        String username = getUsernameFromContext();

        // 记录开始时间
        LocalDateTime operationTime = LocalDateTime.now();

        // 执行目标的方法
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            log.error("操作执行失败: {}", e.getMessage(), e);
            throw e;
        } finally {
            // 异步记录日志
            try {
                // 在finally块中重新获取上下文，因为上下文是在方法执行过程中设置的
                OperationLogContext context = OperationLogContext.get();
                
                if (context == null) {
                    log.warn("操作日志上下文为空: method={}, module={}, type={}", 
                            method.getName(), annotation.module(), annotation.type());
                } else {
                    log.debug("获取到操作日志上下文: projectId={}, bizId={}, bizTitle={}", 
                            context.getProjectId(), context.getBizId(), context.getBizTitle());
                }
                
                recordOperationLogAsync(
                        annotation,
                        joinPoint,
                        userId,
                        username,
                        operationTime,
                        context
                );
            } catch (Exception e) {
                log.error("记录操作日志失败: {}", e.getMessage(), e);
            } finally {
                // 注意：不要在这里清理ThreadLocal，因为异步方法可能需要使用
                // ThreadLocal的清理会在异步方法的TaskDecorator中完成
            }
        }
    }

    /**
     * 异步记录操作日志
     */
    @Async("operationLogTaskExecutor")
    public void recordOperationLogAsync(
            BizOperationLog annotation,
            ProceedingJoinPoint joinPoint,
            Long userId,
            String username,
            LocalDateTime operationTime,
            OperationLogContext context){
        try{
            // 从上下文获取业务信息
            Long projectId = context != null ? context.getProjectId() : null;
            Long bizId = context != null ? context.getBizId() : null;
            String bizTitle = context != null ? context.getBizTitle() : null;

            // 如果上下文中指定了用户信息，这个优先级更高
            if(context != null && context.getUserId() != null){
                userId = context.getUserId();
            }
            if (context != null && context.getUsername() != null) {
                username = context.getUsername();
            }

            // 记录调试信息
            log.info("开始记录操作日志: module={}, type={}, userId={}, username={}, projectId={}, bizId={}, bizTitle={}",
                    annotation.module(), annotation.type(), userId, username, projectId, bizId, bizTitle);

            // 使用日志保存服务统一保存日志
            operationLogSaveCore.saveLogByModule(
                    annotation, projectId, bizId, bizTitle, userId, username, operationTime
            );

            log.info("操作日志记录成功: module={}, type={}, user={}, projectId={}",
                    annotation.module(), annotation.type(), username, projectId);
        }catch (Exception e){
            log.error("异步记录操作日志失败: module={}, type={}, error={}", 
                    annotation.module(), annotation.type(), e.getMessage(), e);
        }
    }

    /**
     * 构建请求参数JSON - 使用工具类优化
     */
    private String buildRequestParams(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < parameterNames.length; i++) {
                Object arg = args[i];
                // 过滤掉HttpServletRequest等不需要序列化的参数
                if (arg instanceof HttpServletRequest ||
                        arg instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                params.put(parameterNames[i], arg);
            }

            // 使用JsonUtils工具类进行序列化
            return JsonUtils.toJsonString(params);
        } catch (Exception e) {
            log.warn("构建请求参数JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从安全上下文获取用户名
     */
    private String getUsernameFromContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getName() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户名失败: {}", e.getMessage());
        }
        return "SYSTEM";
    }
}
