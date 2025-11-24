-- 项目表
CREATE TABLE projects
(
    id          BIGINT PRIMARY KEY COMMENT '项目唯一标识（雪花ID）',
    name        VARCHAR(200)  NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目描述',
    status      ENUM ('PLANNING','ONGOING','COMPLETED','ARCHIVED') DEFAULT 'PLANNING' COMMENT '项目状态（规划中/进行中/已完成/已归档）',
    visibility  ENUM ('PUBLIC','PRIVATE')                          DEFAULT 'PRIVATE' COMMENT '项目可见性（公开/私有）',
    start_date  DATE COMMENT '项目开始日期',
    end_date    DATE COMMENT '项目结束日期',
    image_url   VARCHAR(1000) NOT NULL                             DEFAULT '' COMMENT '项目图片URL',
    creator_id  BIGINT        NOT NULL COMMENT '创建人ID（逻辑关联users表）',
    is_deleted  BOOLEAN                                            DEFAULT FALSE COMMENT '是否已删除（软删除标记）',
    created_at  DATETIME      NOT NULL COMMENT '创建时间',
    updated_at  DATETIME      NOT NULL COMMENT '更新时间',
    created_by  BIGINT COMMENT '创建人ID',
    updated_by  BIGINT COMMENT '最后修改人ID',
    version     INT                                                DEFAULT 0 COMMENT '版本号（乐观锁）',
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_visibility (visibility),
    INDEX idx_is_deleted (is_deleted)
) COMMENT '项目基本信息表';

-- 项目成员表
CREATE TABLE project_members
(
    id                   BIGINT PRIMARY KEY COMMENT '成员记录唯一标识（雪花ID）',
    project_id           BIGINT                          NOT NULL COMMENT '项目ID（逻辑关联projects表）',
    user_id              BIGINT                          NOT NULL COMMENT '用户ID（逻辑关联users表）',
    project_role         ENUM ('OWNER','ADMIN','MEMBER') NOT NULL COMMENT '项目内角色（拥有者/管理员/普通成员）',
    permissions_override JSON COMMENT '权限覆盖（JSON格式，用于临时修改成员在项目内的权限）',
    joined_at            TIMESTAMP                       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入项目时间',
    UNIQUE KEY uk_project_user (project_id, user_id),
    INDEX idx_project_id (project_id),
    INDEX idx_user_id (user_id),
    INDEX idx_project_role (project_role)
) COMMENT '项目成员关系表';

-- 任务表
CREATE TABLE tasks
(
    id              BIGINT PRIMARY KEY COMMENT '任务唯一标识（雪花ID）',
    project_id      BIGINT       NOT NULL COMMENT '所属项目ID（本服务内关联projects表）',
    title           VARCHAR(200) NOT NULL COMMENT '任务标题',
    description     TEXT COMMENT '任务描述',
    worktime        DECIMAL(10, 2) COMMENT '预估工时（单位：小时，支持小数，例如2.5表示2.5小时）',
    status          ENUM ('TODO','IN_PROGRESS','BLOCKED','PENDING_REVIEW','DONE') DEFAULT 'TODO' COMMENT '任务状态（待办/进行中/阻塞/待审核/已完成）',
    priority        ENUM ('HIGH','MEDIUM','LOW')                                  DEFAULT 'MEDIUM' COMMENT '任务优先级（高/中/低）',
    assignee_id     JSON         NOT NULL COMMENT '负责人ID（逻辑关联用户服务的用户ID，可为空表示未分配）JSON类型存储多个负责人ID',
    due_date        DATE COMMENT '任务截止日期',
    required_people INT                                                           DEFAULT 1 COMMENT '任务需要人数',
    is_deleted      TINYINT(1)                                                    DEFAULT 0 COMMENT '是否已删除（软删除标记，FALSE为未删除，TRUE为已删除）',
    is_milestone    TINYINT(1)                                                    DEFAULT 0 COMMENT '是否为里程碑任务（FALSE为普通任务，TRUE为里程碑任务）',
    created_at      DATETIME     NOT NULL COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL COMMENT '更新时间',
    created_by      BIGINT COMMENT '创建人ID',
    updated_by      BIGINT COMMENT '最后修改人ID',
    version         INT                                                           DEFAULT 0 COMMENT '版本号（乐观锁）',
    FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    INDEX idx_project_id (project_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_due_date (due_date),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_is_milestone (is_milestone)
) COMMENT '项目任务表';

-- 任务提交记录表
CREATE TABLE task_submission
(
    id                 BIGINT PRIMARY KEY COMMENT '提交记录ID（雪花ID）',
    task_id            BIGINT                                           NOT NULL COMMENT '任务ID',
    project_id         BIGINT                                           NOT NULL COMMENT '项目ID（冗余字段，提高查询性能）',
    submitter_id       BIGINT                                           NOT NULL COMMENT '提交人ID（执行者ID）',
    submission_type    ENUM ('COMPLETE','PARTIAL','MILESTONE')          NOT NULL DEFAULT 'COMPLETE' COMMENT '提交类型：COMPLETE-完成提交，PARTIAL-阶段性提交，MILESTONE-里程碑提交',
    submission_content TEXT                                             NOT NULL COMMENT '提交说明（必填，描述完成情况）',
    attachment_urls    JSON COMMENT '附件URL列表（可选，JSON数组格式）',
    submission_time    DATETIME                                         NOT NULL COMMENT '提交时间',
    review_status      ENUM ('PENDING','APPROVED','REJECTED','REVOKED') NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING-待审核，APPROVED-已批准，REJECTED-已拒绝，REVOKED-已撤回',
    reviewer_id        BIGINT COMMENT '审核人ID（项目负责人或任务创建者）',
    review_comment     TEXT COMMENT '审核意见（审核人填写）',
    review_time        DATETIME COMMENT '审核时间',
    actual_worktime    DECIMAL(10, 2) COMMENT '实际工时（单位：小时，提交时填写）',
    version            INT                                              NOT NULL DEFAULT 1 COMMENT '提交版本号（同一任务可以多次提交，版本号递增）',
    is_final           BOOLEAN                                          NOT NULL DEFAULT FALSE COMMENT '是否为最终提交（TRUE-任务完成的最终提交）',
    created_at         DATETIME                                         NOT NULL COMMENT '记录创建时间',
    updated_at         DATETIME                                         NOT NULL COMMENT '记录更新时间',
    is_deleted         BOOLEAN                                          NOT NULL DEFAULT FALSE COMMENT '是否已删除（软删除标记）',
    INDEX idx_task_id (task_id),
    INDEX idx_project_id (project_id),
    INDEX idx_submitter_id (submitter_id),
    INDEX idx_review_status (review_status),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_submission_time (submission_time),
    INDEX idx_is_deleted (is_deleted)
) COMMENT '任务提交记录表';

-- 任务用户关联表
CREATE TABLE task_user
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联记录ID',
    task_id     BIGINT                      NOT NULL COMMENT '任务ID',
    project_id  BIGINT                      NOT NULL COMMENT '项目ID（冗余字段，提高查询性能）',
    user_id     BIGINT                      NOT NULL COMMENT '用户ID（执行者ID）',
    assign_type ENUM ('ASSIGNED','CLAIMED') NOT NULL COMMENT '分配类型：ASSIGNED-被管理员分配，CLAIMED-用户主动接取',
    assigned_by BIGINT                      NOT NULL COMMENT '分配人ID（如果是CLAIMED则为user_id本身）',
    assigned_at DATETIME                    NOT NULL COMMENT '分配/接取时间',
    is_active   BOOLEAN                     NOT NULL    DEFAULT TRUE COMMENT '是否有效（TRUE-有效执行者，FALSE-已移除）',
    removed_at  DATETIME COMMENT '移除时间（仅当is_active=FALSE时有值）',
    removed_by  BIGINT COMMENT '移除操作人ID（仅当is_active=FALSE时有值）',
    role_type   ENUM ('EXECUTOR','FOLLOWER','REVIEWER') DEFAULT 'EXECUTOR' COMMENT '角色类型：EXECUTOR-执行者，FOLLOWER-关注者，REVIEWER-审核者',
    notes       VARCHAR(500) COMMENT '备注信息（可记录分配原因等）',
    created_at  DATETIME                    NOT NULL COMMENT '记录创建时间',
    updated_at  DATETIME                    NOT NULL COMMENT '记录更新时间',
    INDEX idx_task_id (task_id),
    INDEX idx_project_id (project_id),
    INDEX idx_user_id (user_id),
    INDEX idx_assign_type (assign_type),
    INDEX idx_is_active (is_active),
    INDEX idx_assigned_at (assigned_at)
) COMMENT '任务用户关联表';

-- 项目加入申请表（根据您的实体类补充）
CREATE TABLE project_join_requests
(
    id           BIGINT PRIMARY KEY COMMENT '申请记录唯一标识（雪花ID）',
    project_id   BIGINT   NOT NULL COMMENT '项目ID（逻辑关联projects表）',
    user_id      BIGINT   NOT NULL COMMENT '申请人ID（逻辑关联users表）',
    status       ENUM ('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' COMMENT '申请状态（待处理/已批准/已拒绝）',
    message      TEXT COMMENT '申请说明（申请人填写的加入理由）',
    responded_at DATETIME COMMENT '处理时间',
    responded_by BIGINT COMMENT '处理人ID（逻辑关联users表），处理人可以是项目负责人或管理员',
    created_at   DATETIME NOT NULL COMMENT '申请时间',
    updated_at   DATETIME NOT NULL COMMENT '更新时间',
    created_by   BIGINT COMMENT '创建人ID',
    updated_by   BIGINT COMMENT '最后修改人ID',
    version      INT                                    DEFAULT 0 COMMENT '版本号（乐观锁）',
    UNIQUE KEY uk_project_user_status (project_id, user_id, status),
    INDEX idx_project_status (project_id, status),
    INDEX idx_user_id (user_id),
    INDEX idx_responded_by (responded_by)
) COMMENT '用户加入项目的申请表';