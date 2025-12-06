package hbnu.project.zhiyanbackend.basic.exception;

import hbnu.project.zhiyanbackend.basic.exception.base.BaseException;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * 服务类异常
 *
 * @author ErgouTree
 */
@Getter
public class ServiceException extends BaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码（数字类型）
     */
    private Integer errorCode;

    /**
     * 错误明细，内部调试错误
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {
        super("业务异常");
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ServiceException(String message, Throwable e) {
        super(message, e);
    }

    public ServiceException(String message, Integer errorCode, Throwable e) {
        super(message, e);
        this.errorCode = errorCode;
    }

    public ServiceException(String s, HttpStatus httpStatus) {
        super();
    }

    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    public ServiceException setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ServiceException setMessage(String message) {
        return this;
    }
}