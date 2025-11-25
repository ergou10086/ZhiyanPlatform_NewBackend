package hbnu.project.zhiyanbackend.auth.exeption;

import hbnu.project.zhiyanbackend.basic.exception.base.BaseException;

/**
 * Cookie异常类
 *
 * @author ErgouTree
 */
public class CookieException extends BaseException {

    private static final long serialVersionUID = 1L;

    /**
     * 模块名称
     */
    private static final String MODULE = "cookie";

    public CookieException(String code, Object[] args, String defaultMessage) {
        super(MODULE, code, args, defaultMessage);
    }

    public CookieException(String code, Object[] args) {
        super(MODULE, code, args);
    }

    public CookieException(String defaultMessage) {
        super(MODULE, defaultMessage);
    }

    public CookieException(String code, String defaultMessage) {
        super(MODULE, code, null, defaultMessage);
    }

    public CookieException(String message, Throwable e) {
        super(message, e);
    }

    /**
     * 创建Cookie解析异常
     *
     * @param cookieName Cookie名称
     * @return CookieException
     */
    public static CookieException parseError(String cookieName) {
        return new CookieException("cookie.parse.error", new Object[]{cookieName}, 
            "解析Cookie '" + cookieName + "' 时发生错误");
    }

    /**
     * 创建Cookie不存在异常
     *
     * @param cookieName Cookie名称
     * @return CookieException
     */
    public static CookieException notFound(String cookieName) {
        return new CookieException("cookie.not.found", new Object[]{cookieName}, 
            "Cookie '" + cookieName + "' 不存在");
    }

    /**
     * 创建Cookie过期异常
     *
     * @param cookieName Cookie名称
     * @return CookieException
     */
    public static CookieException expired(String cookieName) {
        return new CookieException("cookie.expired", new Object[]{cookieName}, 
            "Cookie '" + cookieName + "' 已过期");
    }

    /**
     * 创建Cookie值无效异常
     *
     * @param cookieName Cookie名称
     * @return CookieException
     */
    public static CookieException invalidValue(String cookieName) {
        return new CookieException("cookie.invalid.value", new Object[]{cookieName}, 
            "Cookie '" + cookieName + "' 的值无效");
    }

    /**
     * 创建Cookie设置异常
     *
     * @param cookieName Cookie名称
     * @return CookieException
     */
    public static CookieException setFailed(String cookieName) {
        return new CookieException("cookie.set.failed", new Object[]{cookieName}, 
            "设置Cookie '" + cookieName + "' 失败");
    }

    /**
     * 创建Cookie删除异常
     *
     * @param cookieName Cookie名称
     * @return CookieException
     */
    public static CookieException deleteFailed(String cookieName) {
        return new CookieException("cookie.delete.failed", new Object[]{cookieName}, 
            "删除Cookie '" + cookieName + "' 失败");
    }
}