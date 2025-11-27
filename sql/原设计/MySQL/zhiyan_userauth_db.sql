-- 用户表
CREATE TABLE users
(
    id            BIGINT PRIMARY KEY COMMENT '用户唯一标识（雪花ID）',
    email         VARCHAR(255) UNIQUE NOT NULL COMMENT '用户邮箱（登录账号）',
    password_hash VARCHAR(255)        NOT NULL COMMENT '密码哈希值（加密存储）',
    name          VARCHAR(100)        NOT NULL COMMENT '用户姓名',
    avatar_url    VARCHAR(500) COMMENT '用户头像URL',
    title         VARCHAR(100) COMMENT '用户职称/职位',
    institution   VARCHAR(200) COMMENT '用户所属机构',
    is_locked     BOOLEAN     DEFAULT FALSE COMMENT '是否锁定（禁止登录）',
    is_deleted    BOOLEAN     DEFAULT FALSE COMMENT '软删除标记',
    research_tags JSON COMMENT '研究方向标签（JSON数组，最多5个）',
    status        VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '用户状态（枚举：ACTIVE/LOCKED/DISABLED/DELETED）',
    created_at    DATETIME            NOT NULL COMMENT '创建时间',
    updated_at    DATETIME            NOT NULL COMMENT '更新时间',
    created_by    BIGINT COMMENT '创建人ID',
    updated_by    BIGINT COMMENT '最后修改人ID',
    version       INT         DEFAULT 0 COMMENT '版本号（乐观锁）'
) COMMENT '系统用户基本信息表';

-- 角色表
CREATE TABLE roles
(
    id                BIGINT PRIMARY KEY COMMENT '角色唯一标识（雪花ID）',
    name              VARCHAR(50) UNIQUE NOT NULL COMMENT '角色名称（如：ADMIN、USER）',
    description       TEXT COMMENT '角色描述（如：系统管理员、普通用户）',
    role_type         VARCHAR(20)        NOT NULL COMMENT '角色类型（SYSTEM/PROJECT）',
    project_id        BIGINT COMMENT '项目ID（项目角色时使用）',
    is_system_default BOOLEAN DEFAULT FALSE COMMENT '是否为系统默认角色',
    created_at        DATETIME           NOT NULL COMMENT '创建时间',
    updated_at        DATETIME           NOT NULL COMMENT '更新时间',
    created_by        BIGINT COMMENT '创建人ID',
    updated_by        BIGINT COMMENT '最后修改人ID',
    version           INT     DEFAULT 0 COMMENT '版本号（乐观锁）'
) COMMENT '系统角色定义表';

-- 权限表
CREATE TABLE permissions
(
    id          BIGINT PRIMARY KEY COMMENT '权限唯一标识',
    name        VARCHAR(100) UNIQUE NOT NULL COMMENT '权限名称',
    description TEXT COMMENT '权限描述',
    created_at  DATETIME            NOT NULL COMMENT '创建时间',
    updated_at  DATETIME            NOT NULL COMMENT '更新时间',
    created_by  BIGINT COMMENT '创建人ID',
    updated_by  BIGINT COMMENT '最后修改人ID',
    version     INT DEFAULT 0 COMMENT '版本号（乐观锁）'
) COMMENT '系统权限定义表';

-- 用户角色关联表
CREATE TABLE user_roles
(
    id          BIGINT PRIMARY KEY COMMENT '关联记录唯一标识（雪花ID）',
    user_id     BIGINT NOT NULL COMMENT '用户ID（关联users表）',
    role_id     BIGINT NOT NULL COMMENT '角色ID（关联roles表）',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '角色分配时间',
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    UNIQUE KEY UK_USER_ROLE (user_id, role_id)
) COMMENT '用户与角色的多对多关联表';

-- 角色权限关联表
CREATE TABLE role_permissions
(
    id            BIGINT PRIMARY KEY COMMENT '关联记录唯一标识（雪花ID）',
    role_id       BIGINT NOT NULL COMMENT '角色ID（关联roles表）',
    permission_id BIGINT NOT NULL COMMENT '权限ID（关联permissions表）',
    granted_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '权限授予时间',
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE,
    UNIQUE KEY UK_ROLE_PERMISSION (role_id, permission_id)
) COMMENT '角色与权限的多对多关联表';

-- RememberMe Token表
CREATE TABLE remember_me_tokens
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Token唯一标识',
    user_id      BIGINT              NOT NULL COMMENT '用户ID（关联users表）',
    token        VARCHAR(128) UNIQUE NOT NULL COMMENT 'RememberMe Token值',
    expiry_time  DATETIME            NOT NULL COMMENT 'Token过期时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Token创建时间',
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) COMMENT 'RememberMe自动登录Token表';

-- 用户成果关联表
CREATE TABLE user_achievement
(
    id             BIGINT PRIMARY KEY COMMENT '关联记录ID（雪花ID）',
    user_id        BIGINT   NOT NULL COMMENT '用户ID',
    achievement_id BIGINT   NOT NULL COMMENT '成果ID',
    project_id     BIGINT   NOT NULL COMMENT '所属项目ID',
    display_order  INT DEFAULT 0 COMMENT '展示顺序（用户可自定义排序）',
    remark         VARCHAR(500) COMMENT '备注说明（用户对该成果的个人说明）',
    created_at     DATETIME NOT NULL COMMENT '创建时间',
    updated_at     DATETIME NOT NULL COMMENT '更新时间',
    created_by     BIGINT COMMENT '创建人ID',
    updated_by     BIGINT COMMENT '最后修改人ID',
    version        INT DEFAULT 0 COMMENT '版本号（乐观锁）',
    UNIQUE KEY uk_user_achievement (user_id, achievement_id),
    INDEX idx_user_id (user_id),
    INDEX idx_achievement_id (achievement_id),
    INDEX idx_project_id (project_id)
) COMMENT '用户学术成果关联表';

-- 验证码表
CREATE TABLE verification_codes
(
    id         BIGINT PRIMARY KEY COMMENT '验证码唯一标识（雪花ID）',
    email      VARCHAR(255) NOT NULL COMMENT '用户邮箱',
    code       VARCHAR(10)  NOT NULL COMMENT '验证码',
    type       VARCHAR(20)  NOT NULL COMMENT '验证码类型（REGISTER/RESET_PASSWORD/CHANGE_EMAIL）',
    expires_at DATETIME     NOT NULL COMMENT '验证码过期时间',
    is_used    BOOLEAN DEFAULT FALSE COMMENT '验证码是否已使用',
    created_at DATETIME     NOT NULL COMMENT '创建时间'
) COMMENT '验证码存储表';

-- 创建必要的索引
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_roles_role_type ON roles (role_type);
CREATE INDEX idx_remember_me_tokens_token ON remember_me_tokens (token);
CREATE INDEX idx_verification_codes_email_type ON verification_codes (email, type);