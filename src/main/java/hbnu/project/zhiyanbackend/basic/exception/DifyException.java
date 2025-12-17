package hbnu.project.zhiyanbackend.basic.exception;

import hbnu.project.zhiyanbackend.basic.exception.base.BaseException;
import lombok.Getter;

/**
 * Dify相关业务异常
 *
 * @author ErgouTree
 */
@Getter
public class DifyException extends BaseException {

    // 异常码（改为String类型，与父类一致）
    private final String code;

    public DifyException(String message) {
        this(message, "500");
    }

    public DifyException(String message, String code) { // 参数类型改为String
        super(message);
        this.code = code;
    }

    public DifyException(String message, Throwable cause) {
        this(message, cause, "500");
    }

    public DifyException(String message, Throwable cause, String code) { // 参数类型改为String
        super(message, cause);
        this.code = code;
    }
}