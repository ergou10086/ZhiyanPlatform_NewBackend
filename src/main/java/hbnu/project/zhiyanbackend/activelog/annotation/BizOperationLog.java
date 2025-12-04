package hbnu.project.zhiyanbackend.activelog.annotation;

import java.lang.annotation.*;

import hbnu.project.zhiyanbackend.activelog.model.enums.BizOperationModule;

/**
 * 业务操作日志注解
 * 不使用SpEL表达式，改用直接参数传递
 *
 * @author ErgouTree
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BizOperationLog {

    /**
     * 业务域：PROJECT / TASK / WIKI / ACHIEVEMENT
     */
    BizOperationModule module();

    /**
     * 操作类型
     * 统一字符串
     * 具体会映射到各自域的枚举 valueOf，如 CREATE/UPDATE/DELETE...
     */
    String type();

    /**
     * 操作描述,可方便前端展示
     */
    String description() default "";

    /**
     * 是否记录入参
     */
    boolean recordParams() default true;
}
