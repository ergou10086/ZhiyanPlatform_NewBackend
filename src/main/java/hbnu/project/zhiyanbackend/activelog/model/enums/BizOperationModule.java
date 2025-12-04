package hbnu.project.zhiyanbackend.activelog.model.enums;

import hbnu.project.zhiyanbackend.activelog.annotation.BizOperationLog;

/**
 * 业务域
 * 供 {@link  BizOperationLog}使用
 */
public enum BizOperationModule {

    PROJECT("项目管理"),
    TASK("任务管理"),
    WIKI("Wiki管理"),
    ACHIEVEMENT("成果管理");

    private final String displayName;

    BizOperationModule(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

