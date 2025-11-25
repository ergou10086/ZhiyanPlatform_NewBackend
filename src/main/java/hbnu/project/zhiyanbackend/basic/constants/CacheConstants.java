package hbnu.project.zhiyanbackend.basic.constants;

/**
 * Redis缓存键常量
 * 定义系统中所有Redis缓存的键前缀
 *
 * @author ErgouTree
 */
public class CacheConstants {

    /**
     * 用户Token缓存前缀
     * 格式：user:token:{userId}
     */
    public static final String USER_TOKEN_PREFIX = "user:token:";

    /**
     * Token黑名单前缀
     * 格式：token:blacklist:{token}
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";


    // 角色缓存相关常量
    public static final String USER_ROLES_CACHE_PREFIX = "user:roles:";
    public static final String ROLE_CACHE_PREFIX = "role:";
    public static final String ROLE_PERMISSIONS_CACHE_PREFIX = "role:permissions:";
    public static final String USER_PERMISSIONS_CACHE_PREFIX = "user:permissions:";
    public static final String USER_PERMISSIONS_CACHE_PATTERN = "user:permissions:*";
    public static final long CACHE_EXPIRE_TIME = 1800L;


    // 权限缓存相关常量
    public static final String PERMISSION_CACHE_PREFIX = "permission:";

    // 验证码Redis键前缀
    public static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    public static final String RATE_LIMIT_PREFIX = "rate_limit:verification_code:";
    public static final String USED_CODE_PREFIX = "used_verification_code:";
}

