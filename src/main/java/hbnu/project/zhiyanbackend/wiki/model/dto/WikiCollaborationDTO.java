package hbnu.project.zhiyanbackend.wiki.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Wiki 协同编辑数据传输对象
 *
 * 和旧架构保持兼容，方便 WebSocket/SSE 等实时模块直接复用。
 *
 * @author ErgouTree
 */
public class WikiCollaborationDTO {

    /**
     * 编辑位置信息
     */
    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CursorPosition {
        private Long userId;
        private String username;
        private String avatar;
        private Integer line;
        private Integer column;
        private Integer selectionStart;
        private Integer selectionEnd;
        private LocalDateTime lastUpdate;
        private String paragraphId;
    }

    /**
     * 在线编辑者信息
     */
    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditorInfo {
        private Long userId;
        private String username;
        private String avatar;
        private LocalDateTime joinTime;
    }

    /**
     * 编辑内容变更消息
     */
    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentChange {
        private Long pageId;
        private Long userId;
        private String content;
        private Integer version;
        private LocalDateTime timestamp;
    }

    /**
     * 增量编辑操作（用于实时同步）
     */
    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncrementalChange {
        private String operation;
        private Integer position;
        private String text;
        private Integer length;
        private Long userId;
        private LocalDateTime timestamp;
    }

    /**
     * 编辑状态同步消息
     */
    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncMessage {
        private String type;
        private Long pageId;
        private Long userId;
        private Object data;
        private LocalDateTime timestamp;
    }
}

