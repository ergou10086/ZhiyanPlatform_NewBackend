-- 成果主表
CREATE TABLE achievement
(
    id         BIGINT PRIMARY KEY COMMENT '成果唯一标识（雪花ID）',
    project_id BIGINT                                                                          NOT NULL COMMENT '所属项目ID',
    type       ENUM ('paper', 'patent', 'dataset', 'model', 'report', 'custom', 'task_result') NOT NULL COMMENT '成果类型',
    title      VARCHAR(50)                                                                     NOT NULL COMMENT '成果标题',
    is_public  BOOLEAN                                                                         NOT NULL DEFAULT FALSE COMMENT '成果的公开性',
    creator_id BIGINT                                                                          NOT NULL COMMENT '创建者ID',
    status     ENUM ('draft', 'under_review', 'published', 'obsolete')                         NOT NULL DEFAULT 'draft' COMMENT '状态',
    created_at DATETIME                                                                        NOT NULL COMMENT '创建时间',
    updated_at DATETIME                                                                        NOT NULL COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '最后修改人ID',
    version    INT                                                                                      DEFAULT 0 COMMENT '版本号（乐观锁）',
    INDEX idx_project_status (project_id, status),
    INDEX idx_creator (creator_id),
    INDEX idx_type (type),
    INDEX idx_is_public (is_public)
) COMMENT ='成果主表';

-- 成果详情表
CREATE TABLE achievement_detail
(
    id             BIGINT PRIMARY KEY COMMENT '详情唯一标识（雪花ID）',
    achievement_id BIGINT   NOT NULL UNIQUE COMMENT '关联的成果ID',
    detail_data    JSON     NOT NULL COMMENT '详细信息JSON',
    abstract       TEXT COMMENT '摘要/描述（冗余存储，便于搜索）',
    created_at     DATETIME NOT NULL COMMENT '创建时间',
    updated_at     DATETIME NOT NULL COMMENT '更新时间',
    created_by     BIGINT COMMENT '创建人ID',
    updated_by     BIGINT COMMENT '最后修改人ID',
    version        INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    FOREIGN KEY (achievement_id) REFERENCES achievement (id) ON DELETE CASCADE,
    INDEX idx_achievement_id (achievement_id)
) COMMENT ='成果详情表';

-- 成果文件表
CREATE TABLE achievement_file
(
    id             BIGINT PRIMARY KEY COMMENT '文件唯一标识（雪花ID）',
    achievement_id BIGINT        NOT NULL COMMENT '所属成果ID',
    file_name      VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    file_size      BIGINT COMMENT '文件大小（字节）',
    file_type      VARCHAR(50) COMMENT '文件类型（pdf/zip/csv等）',
    bucket_name    VARCHAR(100)  NOT NULL COMMENT 'MinIO桶名',
    object_key     VARCHAR(500)  NOT NULL COMMENT 'MinIO对象键',
    minio_url      VARCHAR(1000) NOT NULL COMMENT '完整访问URL',
    upload_by      BIGINT        NOT NULL COMMENT '上传者ID',
    upload_at      DATETIME      NOT NULL COMMENT '上传时间',
    INDEX idx_achievement_id (achievement_id),
    INDEX idx_upload_by (upload_by),
    INDEX idx_upload_at (upload_at),
    FOREIGN KEY (achievement_id) REFERENCES achievement (id) ON DELETE CASCADE
) COMMENT ='成果文件表';

-- 成果-任务关联表
CREATE TABLE achievement_task_ref
(
    id             BIGINT PRIMARY KEY COMMENT '关联ID（雪花ID）',
    achievement_id BIGINT   NOT NULL COMMENT '成果ID',
    task_id        BIGINT   NOT NULL COMMENT '任务ID（跨数据库关联）',
    remark         VARCHAR(500) COMMENT '关联备注（可选，用于记录关联原因或说明）',
    created_at     DATETIME NOT NULL COMMENT '创建时间',
    updated_at     DATETIME NOT NULL COMMENT '更新时间',
    created_by     BIGINT COMMENT '创建人ID',
    updated_by     BIGINT COMMENT '最后修改人ID',
    version        INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    UNIQUE KEY uk_achievement_task (achievement_id, task_id),
    INDEX idx_achievement_id (achievement_id),
    INDEX idx_task_id (task_id),
    FOREIGN KEY (achievement_id) REFERENCES achievement (id) ON DELETE CASCADE
) COMMENT ='成果-任务关联表';

-- 成果文件分片上传会话表
CREATE TABLE achievement_file_upload_session
(
    id                   BIGINT PRIMARY KEY COMMENT '分片唯一标识（雪花ID）',
    upload_id            VARCHAR(255)                                             NOT NULL UNIQUE COMMENT 'MinIO的uploadId',
    achievement_id       BIGINT                                                   NOT NULL COMMENT '成果ID',
    file_name            VARCHAR(255)                                             NOT NULL COMMENT '文件名',
    file_size            BIGINT                                                   NOT NULL COMMENT '文件总大小（字节）',
    chunk_size           INT                                                      NOT NULL COMMENT '分片大小（字节）',
    total_chunks         INT                                                      NOT NULL COMMENT '总分片数',
    uploaded_chunks_json TEXT COMMENT '已上传分片号列表（JSON格式）',
    object_key           VARCHAR(500)                                             NOT NULL COMMENT 'MinIO对象键',
    bucket_name          VARCHAR(100)                                             NOT NULL COMMENT '桶名称',
    upload_by            BIGINT                                                   NOT NULL COMMENT '上传用户ID',
    status               ENUM ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL COMMENT '状态',
    created_at           DATETIME                                                 NOT NULL COMMENT '创建时间',
    updated_at           DATETIME                                                 NOT NULL COMMENT '更新时间',
    expired_at           DATETIME COMMENT '过期时间',
    INDEX idx_achievement_id (achievement_id),
    INDEX idx_upload_by (upload_by),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_expired_at (expired_at)
) COMMENT ='成果文件分片上传会话表';

-- Wiki页面表
CREATE TABLE wiki_page
(
    id                 BIGINT PRIMARY KEY COMMENT 'Wiki页面唯一标识（雪花ID）',
    project_id         BIGINT       NOT NULL COMMENT '所属项目ID',
    title              VARCHAR(255) NOT NULL COMMENT '页面标题',
    page_type          VARCHAR(20)  NOT NULL DEFAULT 'DOCUMENT' COMMENT '页面类型',
    mongo_content_id   VARCHAR(24) COMMENT 'MongoDB文档ID',
    parent_id          BIGINT COMMENT '父页面ID',
    path               VARCHAR(1000) COMMENT '页面路径',
    sort_order         INT                   DEFAULT 0 COMMENT '排序序号',
    is_public          BOOLEAN               DEFAULT FALSE COMMENT '是否公开',
    creator_id         BIGINT       NOT NULL COMMENT '创建者ID',
    last_editor_id     BIGINT COMMENT '最后编辑者ID',
    content_size       INT                   DEFAULT 0 COMMENT '内容大小',
    current_version    INT                   DEFAULT 1 COMMENT '当前版本号',
    content_summary    VARCHAR(200) COMMENT '内容摘要',
    is_locked          BOOLEAN               DEFAULT FALSE COMMENT '是否被锁定',
    locked_by          BIGINT COMMENT '锁定者用户ID',
    locked_at          DATETIME COMMENT '锁定时间',
    collaborative_mode BOOLEAN               DEFAULT FALSE COMMENT '是否启用协同编辑',
    created_at         DATETIME     NOT NULL COMMENT '创建时间',
    updated_at         DATETIME     NOT NULL COMMENT '更新时间',
    created_by         BIGINT COMMENT '创建人ID',
    updated_by         BIGINT COMMENT '最后修改人ID',
    version            INT                   DEFAULT 0 COMMENT '版本号（乐观锁）',
    INDEX idx_project_id (project_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_project_parent (project_id, parent_id),
    INDEX idx_project_type (project_id, page_type),
    INDEX idx_mongo_content_id (mongo_content_id),
    INDEX idx_path (path(255)),
    INDEX idx_creator_id (creator_id),
    INDEX idx_is_public (is_public),
    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) COMMENT ='Wiki页面表（存储元数据和关系）';

-- 成果评审记录表（根据实体类 补充）
CREATE TABLE achievement_review
(
    id             BIGINT PRIMARY KEY COMMENT '评审记录唯一标识（雪花ID）',
    achievement_id BIGINT   NOT NULL COMMENT '成果ID',
    reviewer_id    BIGINT   NOT NULL COMMENT '评审人ID',
    review_status  ENUM ('pending', 'approved', 'rejected') DEFAULT 'pending' COMMENT '评审状态',
    comment        TEXT COMMENT '评审意见',
    review_at      DATETIME NOT NULL COMMENT '评审时间',
    created_at     DATETIME NOT NULL COMMENT '创建时间',
    updated_at     DATETIME NOT NULL COMMENT '更新时间',
    created_by     BIGINT COMMENT '创建人ID',
    updated_by     BIGINT COMMENT '最后修改人ID',
    version        INT                                      DEFAULT 0 COMMENT '版本号（乐观锁）',
    INDEX idx_achievement_id (achievement_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_review_status (review_status),
    FOREIGN KEY (achievement_id) REFERENCES achievement (id) ON DELETE CASCADE
) COMMENT ='成果评审记录表';