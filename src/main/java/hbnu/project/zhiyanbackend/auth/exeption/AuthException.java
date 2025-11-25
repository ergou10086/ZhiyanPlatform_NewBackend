package hbnu.project.zhiyanbackend.auth.exeption;

import hbnu.project.zhiyanbackend.basic.exception.base.BaseException;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;

/**
 * 统一认证异常类
 * 整合了登录、权限、角色、用户信息等相关异常
 *
 * @author ErgouTree, yui
 */
public class AuthException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    // 模块名称
    private static final String MODULE = "auth";

    /**
     * 错误码常量
     */
    // 登录相关
    public static final String NOT_LOGIN = "not_login";
    public static final String USER_PASSWORD_NOT_MATCH = "user_password_not_match";

    // 权限相关
    public static final String NOT_PERMISSION = "not_permission";
    public static final String NOT_ROLE = "not_role";

    // 用户相关
    public static final String USER_NOT_FOUND = "user_not_found";
    public static final String USER_LOCKED = "user_locked";
    public static final String USER_DISABLED = "user_disabled";

    // 操作相关
    public static final String AVATAR_UPLOAD_FAILED = "avatar_upload_failed";
    public static final String PARAM_VALIDATION_FAILED = "param_validation_failed";
    public static final String OPERATION_NOT_ALLOWED = "operation_not_allowed";

    /**
     * 异常类型枚举
     */
    public enum ExceptionType {
        NOT_LOGIN,
        NOT_PERMISSION,
        NOT_ROLE,
        USER_PASSWORD_NOT_MATCH,
        USER_NOT_FOUND,
        USER_LOCKED,
        USER_DISABLED,
        AVATAR_UPLOAD_FAILED,
        PARAM_VALIDATION_FAILED,
        OPERATION_NOT_ALLOWED,
        CUSTOM
    }

    private final ExceptionType exceptionType;

    public AuthException(String code, Object[] args, String defaultMessage) {
        super(MODULE, code, args, defaultMessage);
        this.exceptionType = ExceptionType.CUSTOM;
    }

    public AuthException(String code, String defaultMessage) {
        this(code, null, defaultMessage);
    }

    public AuthException(String defaultMessage) {
        super(MODULE, defaultMessage);
        this.exceptionType = ExceptionType.CUSTOM;
    }

    public AuthException(String message, Throwable e) {
        super(message, e);
        this.exceptionType = ExceptionType.CUSTOM;
    }

    public AuthException(ExceptionType type, String message) {
        super(MODULE, getCodeByType(type), null, message);
        this.exceptionType = type;
    }

    public AuthException(ExceptionType type, String code, Object[] args, String message) {
        super(MODULE, code, args, message);
        this.exceptionType = type;
    }

    /**
     * 获取异常类型
     */
    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    /**
     * 根据异常类型获取错误码
     */
    private static String getCodeByType(ExceptionType type) {
        switch (type) {
            case NOT_LOGIN:
                return NOT_LOGIN;
            case NOT_PERMISSION:
                return NOT_PERMISSION;
            case NOT_ROLE:
                return NOT_ROLE;
            case USER_PASSWORD_NOT_MATCH:
                return USER_PASSWORD_NOT_MATCH;
            case USER_NOT_FOUND:
                return USER_NOT_FOUND;
            case USER_LOCKED:
                return USER_LOCKED;
            case USER_DISABLED:
                return USER_DISABLED;
            case AVATAR_UPLOAD_FAILED:
                return AVATAR_UPLOAD_FAILED;
            case PARAM_VALIDATION_FAILED:
                return PARAM_VALIDATION_FAILED;
            case OPERATION_NOT_ALLOWED:
                return OPERATION_NOT_ALLOWED;
            default:
                return "unknown";
        }
    }

    // ==================== 快速创建方法 ====================

    /**
     * 未能通过的登录认证异常
     */
    public static AuthException notLogin(String message) {
        return new AuthException(ExceptionType.NOT_LOGIN, NOT_LOGIN, null,
                message != null ? message : "用户未登录或登录已过期");
    }

    public static AuthException notLogin() {
        return notLogin(null);
    }

    /**
     * 未能通过的权限认证异常
     */
    public static AuthException notPermission(String permission) {
        return new AuthException(ExceptionType.NOT_PERMISSION, NOT_PERMISSION,
                new Object[]{permission}, "没有访问权限: " + permission);
    }

    public static AuthException notPermission(String[] permissions) {
        return new AuthException(ExceptionType.NOT_PERMISSION, NOT_PERMISSION,
                new Object[]{StringUtils.join(permissions, ",")},
                "没有访问权限: " + StringUtils.join(permissions, ","));
    }

    /**
     * 未能通过的角色认证异常
     */
    public static AuthException notRole(String role) {
        return new AuthException(ExceptionType.NOT_ROLE, NOT_ROLE,
                new Object[]{role}, "没有角色权限: " + role);
    }

    public static AuthException notRole(String[] roles) {
        return new AuthException(ExceptionType.NOT_ROLE, NOT_ROLE,
                new Object[]{StringUtils.join(roles, ",")},
                "没有角色权限: " + StringUtils.join(roles, ","));
    }

    /**
     * 用户密码不正确异常
     */
    public static AuthException userPasswordNotMatch() {
        return new AuthException(ExceptionType.USER_PASSWORD_NOT_MATCH,
                USER_PASSWORD_NOT_MATCH, null, "用户密码不正确或不符合规范");
    }

    /**
     * 用户不存在异常
     */
    public static AuthException userNotFound(Long userId) {
        return new AuthException(ExceptionType.USER_NOT_FOUND, USER_NOT_FOUND,
                new Object[]{userId}, "用户不存在: " + userId);
    }

    public static AuthException userNotFound(String username) {
        return new AuthException(ExceptionType.USER_NOT_FOUND, USER_NOT_FOUND,
                new Object[]{username}, "用户不存在: " + username);
    }

    /**
     * 用户被锁定异常
     */
    public static AuthException userLocked(String username) {
        return new AuthException(ExceptionType.USER_LOCKED, USER_LOCKED,
                new Object[]{username}, "用户已被锁定: " + username);
    }

    /**
     * 用户被禁用异常
     */
    public static AuthException userDisabled(String username) {
        return new AuthException(ExceptionType.USER_DISABLED, USER_DISABLED,
                new Object[]{username}, "用户已被禁用: " + username);
    }

    /**
     * 头像上传失败异常
     */
    public static AuthException avatarUploadFailed(String message) {
        return new AuthException(ExceptionType.AVATAR_UPLOAD_FAILED, AVATAR_UPLOAD_FAILED,
                new Object[]{message}, "头像上传失败: " + message);
    }

    /**
     * 参数验证失败异常
     */
    public static AuthException paramValidationFailed(String field, String message) {
        return new AuthException(ExceptionType.PARAM_VALIDATION_FAILED, PARAM_VALIDATION_FAILED,
                new Object[]{field, message}, "参数验证失败[" + field + "]: " + message);
    }

    /**
     * 操作不允许异常
     */
    public static AuthException operationNotAllowed(String operation) {
        return new AuthException(ExceptionType.OPERATION_NOT_ALLOWED, OPERATION_NOT_ALLOWED,
                new Object[]{operation}, "操作不允许: " + operation);
    }

    /**
     * 自定义异常
     */
    public static AuthException custom(String code, String message) {
        return new AuthException(code, message);
    }

    public static AuthException custom(String code, Object[] args, String message) {
        return new AuthException(code, args, message);
    }

    // ==================== 便捷判断方法 ====================

    /**
     * 判断是否为登录异常
     */
    public boolean isNotLogin() {
        return ExceptionType.NOT_LOGIN.equals(exceptionType);
    }

    /**
     * 判断是否为权限异常
     */
    public boolean isNotPermission() {
        return ExceptionType.NOT_PERMISSION.equals(exceptionType);
    }

    /**
     * 判断是否为角色异常
     */
    public boolean isNotRole() {
        return ExceptionType.NOT_ROLE.equals(exceptionType);
    }

    /**
     * 判断是否为密码不匹配异常
     */
    public boolean isUserPasswordNotMatch() {
        return ExceptionType.USER_PASSWORD_NOT_MATCH.equals(exceptionType);
    }

    /**
     * 判断是否为用户不存在异常
     */
    public boolean isUserNotFound() {
        return ExceptionType.USER_NOT_FOUND.equals(exceptionType);
    }
}