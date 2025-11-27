-- ============================================
-- 智研平台认证模块数据库设计
-- 数据库：PostgreSQL
-- Schema：zhiyanauth（单库Schema隔离）
-- 说明：只包含系统角色相关的表结构
-- ============================================

-- 创建Schema
CREATE SCHEMA IF NOT EXISTS zhiyanauth;

-- 设置搜索路径
SET search_path TO zhiyanauth, public;

-- ============================================
-- 1. 用户表 (users)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.users (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    avatar_data BYTEA,
    avatar_content_type VARCHAR(50),
    avatar_size BIGINT,
    title VARCHAR(100),
    institution VARCHAR(200),
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    research_tags JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0
);

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_users_email ON zhiyanauth.users(email);
CREATE INDEX IF NOT EXISTS idx_users_status ON zhiyanauth.users(status);
CREATE INDEX IF NOT EXISTS idx_users_is_deleted ON zhiyanauth.users(is_deleted);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON zhiyanauth.users(created_at);

-- 用户表注释
COMMENT ON TABLE zhiyanauth.users IS '用户表';
COMMENT ON COLUMN zhiyanauth.users.id IS '用户唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.users.email IS '用户邮箱（登录账号）';
COMMENT ON COLUMN zhiyanauth.users.password_hash IS '密码哈希值（加密存储）';
COMMENT ON COLUMN zhiyanauth.users.name IS '用户姓名';
COMMENT ON COLUMN zhiyanauth.users.avatar_data IS '用户头像二进制数据（PostgreSQL BYTEA类型）';
COMMENT ON COLUMN zhiyanauth.users.avatar_content_type IS '头像MIME类型（如：image/jpeg, image/png）';
COMMENT ON COLUMN zhiyanauth.users.avatar_size IS '头像文件大小（字节）';
COMMENT ON COLUMN zhiyanauth.users.title IS '用户职称/职位';
COMMENT ON COLUMN zhiyanauth.users.institution IS '用户所属机构';
COMMENT ON COLUMN zhiyanauth.users.is_locked IS '是否锁定（禁止登录）';
COMMENT ON COLUMN zhiyanauth.users.is_deleted IS '软删除标记';
COMMENT ON COLUMN zhiyanauth.users.research_tags IS '研究方向标签（JSON数组，最多5个）';
COMMENT ON COLUMN zhiyanauth.users.status IS '用户状态（枚举：ACTIVE/LOCKED/DISABLED/DELETED）';
COMMENT ON COLUMN zhiyanauth.users.created_at IS '创建时间';
COMMENT ON COLUMN zhiyanauth.users.updated_at IS '更新时间';
COMMENT ON COLUMN zhiyanauth.users.created_by IS '创建人ID';
COMMENT ON COLUMN zhiyanauth.users.updated_by IS '最后修改人ID';
COMMENT ON COLUMN zhiyanauth.users.version IS '版本号（乐观锁）';

-- ============================================
-- 2. 角色表 (roles)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.roles (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    role_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    is_system_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0
);

-- 角色表索引
CREATE INDEX IF NOT EXISTS idx_roles_name ON zhiyanauth.roles(name);
CREATE INDEX IF NOT EXISTS idx_roles_role_type ON zhiyanauth.roles(role_type);
CREATE INDEX IF NOT EXISTS idx_roles_is_system_default ON zhiyanauth.roles(is_system_default);

-- 角色表注释
COMMENT ON TABLE zhiyanauth.roles IS '角色表（系统角色）';
COMMENT ON COLUMN zhiyanauth.roles.id IS '角色唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.roles.name IS '角色名称（如：DEVELOPER、USER、GUEST）';
COMMENT ON COLUMN zhiyanauth.roles.description IS '角色描述（如：系统管理员、普通用户）';
COMMENT ON COLUMN zhiyanauth.roles.role_type IS '角色类型（固定为SYSTEM）';
COMMENT ON COLUMN zhiyanauth.roles.is_system_default IS '是否为系统默认角色';
COMMENT ON COLUMN zhiyanauth.roles.created_at IS '创建时间';
COMMENT ON COLUMN zhiyanauth.roles.updated_at IS '更新时间';
COMMENT ON COLUMN zhiyanauth.roles.created_by IS '创建人ID';
COMMENT ON COLUMN zhiyanauth.roles.updated_by IS '最后修改人ID';
COMMENT ON COLUMN zhiyanauth.roles.version IS '版本号（乐观锁）';

-- ============================================
-- 3. 权限表 (permissions)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.permissions (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0
);

-- 权限表索引
CREATE INDEX IF NOT EXISTS idx_permissions_name ON zhiyanauth.permissions(name);

-- 权限表注释
COMMENT ON TABLE zhiyanauth.permissions IS '权限表';
COMMENT ON COLUMN zhiyanauth.permissions.id IS '权限唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.permissions.name IS '权限名称（如：system:user:list）';
COMMENT ON COLUMN zhiyanauth.permissions.description IS '权限描述';
COMMENT ON COLUMN zhiyanauth.permissions.created_at IS '创建时间';
COMMENT ON COLUMN zhiyanauth.permissions.updated_at IS '更新时间';
COMMENT ON COLUMN zhiyanauth.permissions.created_by IS '创建人ID';
COMMENT ON COLUMN zhiyanauth.permissions.updated_by IS '最后修改人ID';
COMMENT ON COLUMN zhiyanauth.permissions.version IS '版本号（乐观锁）';

-- ============================================
-- 4. 用户角色关联表 (user_roles)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.user_roles (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_USER_ROLE_USER FOREIGN KEY (user_id) REFERENCES zhiyanauth.users(id) ON DELETE CASCADE,
    CONSTRAINT FK_USER_ROLE_ROLE FOREIGN KEY (role_id) REFERENCES zhiyanauth.roles(id) ON DELETE CASCADE,
    CONSTRAINT UK_USER_ROLE UNIQUE (user_id, role_id)
);

-- 用户角色关联表索引
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON zhiyanauth.user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON zhiyanauth.user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_assigned_at ON zhiyanauth.user_roles(assigned_at);

-- 用户角色关联表注释
COMMENT ON TABLE zhiyanauth.user_roles IS '用户角色关联表';
COMMENT ON COLUMN zhiyanauth.user_roles.id IS '关联记录唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.user_roles.user_id IS '用户ID';
COMMENT ON COLUMN zhiyanauth.user_roles.role_id IS '角色ID';
COMMENT ON COLUMN zhiyanauth.user_roles.assigned_at IS '角色分配时间';

-- ============================================
-- 5. 角色权限关联表 (role_permissions)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.role_permissions (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_ROLE_PERMISSION_ROLE FOREIGN KEY (role_id) REFERENCES zhiyanauth.roles(id) ON DELETE CASCADE,
    CONSTRAINT FK_ROLE_PERMISSION_PERMISSION FOREIGN KEY (permission_id) REFERENCES zhiyanauth.permissions(id) ON DELETE CASCADE,
    CONSTRAINT UK_ROLE_PERMISSION UNIQUE (role_id, permission_id)
);

-- 角色权限关联表索引
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON zhiyanauth.role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON zhiyanauth.role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_granted_at ON zhiyanauth.role_permissions(granted_at);

-- 角色权限关联表注释
COMMENT ON TABLE zhiyanauth.role_permissions IS '角色权限关联表';
COMMENT ON COLUMN zhiyanauth.role_permissions.id IS '关联记录唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.role_permissions.role_id IS '角色ID';
COMMENT ON COLUMN zhiyanauth.role_permissions.permission_id IS '权限ID';
COMMENT ON COLUMN zhiyanauth.role_permissions.granted_at IS '权限授予时间';

-- ============================================
-- 初始化系统角色数据
-- ============================================
-- 注意：这里使用占位符ID，实际使用时需要通过雪花ID生成器生成
-- 开发者角色
INSERT INTO zhiyanauth.roles (id, name, description, role_type, is_system_default, created_at, updated_at, version)
VALUES (1000000000000000001, 'DEVELOPER', '拥有系统所有权限，包括用户、角色、权限的完全控制', 'SYSTEM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 普通用户角色
INSERT INTO zhiyanauth.roles (id, name, description, role_type, is_system_default, created_at, updated_at, version)
VALUES (1000000000000000002, 'USER', '可以创建项目，管理个人信息，参与项目团队', 'SYSTEM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 访客用户角色
INSERT INTO zhiyanauth.roles (id, name, description, role_type, is_system_default, created_at, updated_at, version)
VALUES (1000000000000000003, 'GUEST', '受限的访问权限，无法创建项目', 'SYSTEM', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 初始化系统权限数据
-- ============================================
-- 基础权限
INSERT INTO zhiyanauth.permissions (id, name, description, created_at, updated_at, version)
VALUES 
    (2000000000000000001, 'profile:manage', '管理个人信息', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000002, 'project:create', '创建新项目', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 用户管理权限
INSERT INTO zhiyanauth.permissions (id, name, description, created_at, updated_at, version)
VALUES 
    (2000000000000000003, 'system:user:list', '查看用户列表', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000004, 'system:user:view', '查看用户详情', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000005, 'system:user:create', '创建用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000006, 'system:user:update', '更新用户信息', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000007, 'system:user:delete', '删除用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000008, 'system:user:lock', '锁定或解锁用户账户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 角色管理权限
INSERT INTO zhiyanauth.permissions (id, name, description, created_at, updated_at, version)
VALUES 
    (2000000000000000009, 'system:role:list', '查看角色列表', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000010, 'system:role:view', '查看角色详情', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000011, 'system:role:create', '创建角色', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000012, 'system:role:update', '更新角色', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000013, 'system:role:delete', '删除角色', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000014, 'system:role:assign', '为用户分配角色', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 权限管理
INSERT INTO zhiyanauth.permissions (id, name, description, created_at, updated_at, version)
VALUES 
    (2000000000000000015, 'system:permission:list', '查看权限列表', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000016, 'system:permission:assign', '为角色分配权限', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 项目权限
INSERT INTO zhiyanauth.permissions (id, name, description, created_at, updated_at, version)
VALUES 
    (2000000000000000017, 'project:manage', '管理项目基本信息、任务、成员', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000018, 'project:delete', '删除项目', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000019, 'knowledge:manage', '管理项目知识库', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- 系统管理权限
INSERT INTO zhiyanauth.permissions (id, name, description, created_at, updated_at, version)
VALUES 
    (2000000000000000020, 'user:admin', '管理系统用户（综合权限）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2000000000000000021, 'system:admin', '系统配置和监控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 6. Remember Me Token表 (remember_me_tokens)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.remember_me_tokens (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    expiry_time TIMESTAMP NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_REMEMBER_ME_TOKEN_USER FOREIGN KEY (user_id) REFERENCES zhiyanauth.users(id) ON DELETE CASCADE
);

-- Remember Me Token表索引
CREATE INDEX IF NOT EXISTS idx_remember_me_tokens_user_id ON zhiyanauth.remember_me_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_remember_me_tokens_token ON zhiyanauth.remember_me_tokens(token);
CREATE INDEX IF NOT EXISTS idx_remember_me_tokens_expiry_time ON zhiyanauth.remember_me_tokens(expiry_time);

-- Remember Me Token表注释
COMMENT ON TABLE zhiyanauth.remember_me_tokens IS 'Remember Me Token表';
COMMENT ON COLUMN zhiyanauth.remember_me_tokens.id IS 'Token唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.remember_me_tokens.user_id IS '用户ID';
COMMENT ON COLUMN zhiyanauth.remember_me_tokens.token IS 'Remember Me Token值';
COMMENT ON COLUMN zhiyanauth.remember_me_tokens.expiry_time IS 'Token过期时间';
COMMENT ON COLUMN zhiyanauth.remember_me_tokens.created_time IS 'Token创建时间';

-- ============================================
-- 7. 用户学术成果关联表 (user_achievements)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.user_achievements (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT FK_USER_ACHIEVEMENT_USER FOREIGN KEY (user_id) REFERENCES zhiyanauth.users(id) ON DELETE CASCADE,
    CONSTRAINT UK_USER_ACHIEVEMENT UNIQUE (user_id, achievement_id)
);

-- 用户学术成果关联表索引
CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON zhiyanauth.user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_achievement_id ON zhiyanauth.user_achievements(achievement_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_project_id ON zhiyanauth.user_achievements(project_id);

-- 用户学术成果关联表注释
COMMENT ON TABLE zhiyanauth.user_achievements IS '用户学术成果关联表';
COMMENT ON COLUMN zhiyanauth.user_achievements.id IS '关联记录ID（雪花ID）';
COMMENT ON COLUMN zhiyanauth.user_achievements.user_id IS '用户ID';
COMMENT ON COLUMN zhiyanauth.user_achievements.achievement_id IS '成果ID（关联知识库模块的成果）';
COMMENT ON COLUMN zhiyanauth.user_achievements.project_id IS '所属项目ID';
COMMENT ON COLUMN zhiyanauth.user_achievements.display_order IS '展示顺序（用户可自定义排序）';
COMMENT ON COLUMN zhiyanauth.user_achievements.remark IS '备注说明（用户对该成果的个人说明）';
COMMENT ON COLUMN zhiyanauth.user_achievements.created_at IS '创建时间';
COMMENT ON COLUMN zhiyanauth.user_achievements.updated_at IS '更新时间';
COMMENT ON COLUMN zhiyanauth.user_achievements.created_by IS '创建人ID';
COMMENT ON COLUMN zhiyanauth.user_achievements.updated_by IS '最后修改人ID';
COMMENT ON COLUMN zhiyanauth.user_achievements.version IS '版本号（乐观锁）';

-- ============================================
-- 8. 验证码表 (verification_codes)
-- ============================================
CREATE TABLE IF NOT EXISTS zhiyanauth.verification_codes (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 验证码表索引
CREATE INDEX IF NOT EXISTS idx_verification_codes_email_type ON zhiyanauth.verification_codes(email, type);
CREATE INDEX IF NOT EXISTS idx_verification_codes_expires_at ON zhiyanauth.verification_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_verification_codes_created_at ON zhiyanauth.verification_codes(created_at);
CREATE INDEX IF NOT EXISTS idx_verification_codes_is_used ON zhiyanauth.verification_codes(is_used);

-- 验证码表注释
COMMENT ON TABLE zhiyanauth.verification_codes IS '验证码表';
COMMENT ON COLUMN zhiyanauth.verification_codes.id IS '验证码唯一标识（雪花ID）';
COMMENT ON COLUMN zhiyanauth.verification_codes.email IS '用户邮箱';
COMMENT ON COLUMN zhiyanauth.verification_codes.code IS '验证码';
COMMENT ON COLUMN zhiyanauth.verification_codes.type IS '验证码类型（REGISTER/RESET_PASSWORD/CHANGE_EMAIL）';
COMMENT ON COLUMN zhiyanauth.verification_codes.expires_at IS '验证码过期时间';
COMMENT ON COLUMN zhiyanauth.verification_codes.is_used IS '验证码是否已使用';
COMMENT ON COLUMN zhiyanauth.verification_codes.created_at IS '创建时间';

