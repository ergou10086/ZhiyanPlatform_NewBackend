-- ============================================
-- 智研平台消息模块数据库设计
-- 数据库：PostgreSQL
-- Schema：public（默认Schema）
-- 说明：消息模块相关表结构
-- ============================================

-- ============================================
-- 1. 消息体表 (message_body)
-- ============================================
CREATE TABLE IF NOT EXISTS message_body (
    -- 主键
    id BIGINT PRIMARY KEY,
    
    -- 消息基本信息
    sender_id BIGINT,
    message_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    scene VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    
    -- 业务关联
    business_id BIGINT,
    business_type VARCHAR(50),
    
    -- 时间字段
    trigger_time TIMESTAMP NOT NULL,
    
    -- 扩展字段（JSONB格式，PostgreSQL推荐使用JSONB）
    extend_data JSONB,
    
    -- 审计字段（继承BaseAuditEntity）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0
);

-- 消息体表索引
CREATE INDEX IF NOT EXISTS idx_scene_time ON message_body(scene, trigger_time);
CREATE INDEX IF NOT EXISTS idx_biz_type ON message_body(business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_sender ON message_body(sender_id);
CREATE INDEX IF NOT EXISTS idx_trigger_time ON message_body(trigger_time);
CREATE INDEX IF NOT EXISTS idx_message_type ON message_body(message_type);
CREATE INDEX IF NOT EXISTS idx_priority ON message_body(priority);

-- 消息体表注释
COMMENT ON TABLE message_body IS '消息体表 - 存储消息的核心内容';
COMMENT ON COLUMN message_body.id IS '消息体ID（雪花ID）';
COMMENT ON COLUMN message_body.sender_id IS '发送人ID（null表示系统消息）';
COMMENT ON COLUMN message_body.message_type IS '消息类型：PERSONAL-个人消息, GROUP-群组消息, BROADCAST-广播消息';
COMMENT ON COLUMN message_body.scene IS '消息场景';
COMMENT ON COLUMN message_body.priority IS '优先级：HIGH-高, MEDIUM-中, LOW-低';
COMMENT ON COLUMN message_body.title IS '消息标题';
COMMENT ON COLUMN message_body.content IS '消息正文';
COMMENT ON COLUMN message_body.business_id IS '业务关联ID（如任务ID、项目ID、成果ID等）';
COMMENT ON COLUMN message_body.business_type IS '业务类型（如"TASK"、"PROJECT"、"ACHIEVEMENT"等）';
COMMENT ON COLUMN message_body.trigger_time IS '消息触发时间（业务发生时间）';
COMMENT ON COLUMN message_body.extend_data IS '扩展字段（JSONB格式，可存储额外的业务数据，如跳转链接、操作按钮等）';
COMMENT ON COLUMN message_body.created_at IS '创建时间';
COMMENT ON COLUMN message_body.updated_at IS '更新时间';
COMMENT ON COLUMN message_body.created_by IS '创建人ID';
COMMENT ON COLUMN message_body.updated_by IS '最后修改人ID';
COMMENT ON COLUMN message_body.version IS '版本号（乐观锁）';

-- ============================================
-- 2. 消息收件人表 (message_recipient)
-- ============================================
CREATE TABLE IF NOT EXISTS message_recipient (
    -- 主键（自增）
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联字段
    message_body_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    
    -- 冗余字段（便于查询）
    scene_code VARCHAR(50),
    
    -- 阅读状态
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    
    -- 时间字段
    trigger_time TIMESTAMP NOT NULL,
    
    -- 软删除
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_message_recipient_message_body 
        FOREIGN KEY (message_body_id) 
        REFERENCES message_body(id) 
        ON DELETE CASCADE
);

-- 消息收件人表索引
CREATE INDEX IF NOT EXISTS idx_receiver_status ON message_recipient(receiver_id, read_flag, deleted);
CREATE INDEX IF NOT EXISTS idx_message_body ON message_recipient(message_body_id);
CREATE INDEX IF NOT EXISTS idx_receiver_scene ON message_recipient(receiver_id, scene_code);
CREATE INDEX IF NOT EXISTS idx_receiver_unread ON message_recipient(receiver_id, read_flag, trigger_time);
CREATE INDEX IF NOT EXISTS idx_trigger_time ON message_recipient(trigger_time);
CREATE INDEX IF NOT EXISTS idx_deleted ON message_recipient(deleted);

-- 消息收件人表注释
COMMENT ON TABLE message_recipient IS '消息收件人表 - 收件人维度的消息记录';
COMMENT ON COLUMN message_recipient.id IS '收件记录ID（主键，自增）';
COMMENT ON COLUMN message_recipient.message_body_id IS '关联消息体ID';
COMMENT ON COLUMN message_recipient.receiver_id IS '接收人ID';
COMMENT ON COLUMN message_recipient.scene_code IS '场景代码（冗余字段，便于查询）';
COMMENT ON COLUMN message_recipient.read_flag IS '是否已读';
COMMENT ON COLUMN message_recipient.read_at IS '读取时间';
COMMENT ON COLUMN message_recipient.trigger_time IS '消息触发时间（冗余字段，便于排序）';
COMMENT ON COLUMN message_recipient.deleted IS '是否已删除（软删除）';
COMMENT ON COLUMN message_recipient.deleted_at IS '删除时间';

-- ============================================
-- 3. 消息发送记录表 (message_send_record)
-- ============================================
CREATE TABLE IF NOT EXISTS message_send_record (
    -- 主键（自增）
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联字段
    message_body_id BIGINT NOT NULL,
    
    -- 发送信息
    send_time TIMESTAMP NOT NULL,
    total_recipients INTEGER NOT NULL,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'SENDING',
    
    -- 审计字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0,
    
    -- 外键约束
    CONSTRAINT fk_message_send_record_message_body 
        FOREIGN KEY (message_body_id) 
        REFERENCES message_body(id) 
        ON DELETE CASCADE
);

-- 消息发送记录表索引
CREATE INDEX IF NOT EXISTS idx_message_body ON message_send_record(message_body_id);
CREATE INDEX IF NOT EXISTS idx_send_time ON message_send_record(send_time);
CREATE INDEX IF NOT EXISTS idx_status ON message_send_record(status);

-- 消息发送记录表注释
COMMENT ON TABLE message_send_record IS '消息发送记录表 - 记录群组消息和广播消息的发送情况';
COMMENT ON COLUMN message_send_record.id IS '发送记录ID（主键，自增）';
COMMENT ON COLUMN message_send_record.message_body_id IS '消息体ID';
COMMENT ON COLUMN message_send_record.send_time IS '发送时间';
COMMENT ON COLUMN message_send_record.total_recipients IS '目标收件人总数';
COMMENT ON COLUMN message_send_record.success_count IS '成功发送数量';
COMMENT ON COLUMN message_send_record.failed_count IS '失败数量';
COMMENT ON COLUMN message_send_record.status IS '发送状态：SENDING-发送中, SUCCESS-成功, PARTIAL_FAILED-部分失败, FAILED-失败';
COMMENT ON COLUMN message_send_record.created_at IS '创建时间';
COMMENT ON COLUMN message_send_record.updated_at IS '更新时间';
COMMENT ON COLUMN message_send_record.created_by IS '创建人ID';
COMMENT ON COLUMN message_send_record.updated_by IS '最后修改人ID';
COMMENT ON COLUMN message_send_record.version IS '版本号（乐观锁）';

-- ============================================
-- 触发器：自动更新 updated_at 字段
-- ============================================

-- 消息体表触发器
CREATE OR REPLACE FUNCTION update_message_body_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_message_body_updated_at
    BEFORE UPDATE ON message_body
    FOR EACH ROW
    EXECUTE FUNCTION update_message_body_updated_at();

-- 消息发送记录表触发器
CREATE OR REPLACE FUNCTION update_message_send_record_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_message_send_record_updated_at
    BEFORE UPDATE ON message_send_record
    FOR EACH ROW
    EXECUTE FUNCTION update_message_send_record_updated_at();

