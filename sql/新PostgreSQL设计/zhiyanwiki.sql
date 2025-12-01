-- =====================================================
-- Wiki系统 PostgreSQL 数据库迁移脚本
-- 从 MySQL + MongoDB 迁移到 PostgreSQL
-- =====================================================

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- UUID生成
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- 三元组索引，用于模糊搜索
CREATE EXTENSION IF NOT EXISTS "btree_gin";      -- GIN索引支持
CREATE EXTENSION IF NOT EXISTS "ltree";          -- 层级树结构支持

-- =====================================================
-- 1. Wiki页面表（包含内容和最近版本）
-- =====================================================
CREATE TABLE IF NOT EXISTS wiki_page (
                                         id BIGINT PRIMARY KEY,
                                         project_id BIGINT NOT NULL,
                                         title VARCHAR(255) NOT NULL,
                                         page_type VARCHAR(20) NOT NULL DEFAULT 'DOCUMENT',
                                         parent_id BIGINT,
                                         path VARCHAR(1000),
                                         sort_order INTEGER DEFAULT 0,
                                         is_public BOOLEAN DEFAULT FALSE,
                                         creator_id BIGINT NOT NULL,
                                         last_editor_id BIGINT,

    -- 内容字段
                                         content TEXT,
                                         content_hash VARCHAR(64),
                                         content_size INTEGER,
                                         current_version INTEGER DEFAULT 1,
                                         content_summary VARCHAR(200),
                                         search_vector tsvector,  -- 全文搜索向量

    -- 版本历史（JSONB存储最近10个版本）
                                         recent_versions JSONB DEFAULT '[]'::jsonb,

    -- 协同编辑字段（预留）
                                         is_locked BOOLEAN DEFAULT FALSE,
                                         locked_by BIGINT,
                                         locked_at TIMESTAMP,
                                         collaborative_mode BOOLEAN DEFAULT FALSE,
                                         active_editors BIGINT[] DEFAULT ARRAY[]::BIGINT[],
                                         operation_sequence BIGINT,
                                         last_sync_at TIMESTAMP,

    -- 审计字段
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         created_by BIGINT,
                                         updated_by BIGINT,

                                         CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES wiki_page(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_wiki_page_project ON wiki_page(project_id);
CREATE INDEX idx_wiki_page_parent ON wiki_page(parent_id);
CREATE INDEX idx_wiki_page_project_parent ON wiki_page(project_id, parent_id);
CREATE INDEX idx_wiki_page_project_type ON wiki_page(project_id, page_type);
CREATE INDEX idx_wiki_page_path ON wiki_page USING btree(path);
CREATE INDEX idx_wiki_page_updated_at ON wiki_page(updated_at DESC);

-- GIN索引用于JSONB查询
CREATE INDEX idx_wiki_page_recent_versions ON wiki_page USING gin(recent_versions);

-- 全文搜索索引（支持中文和英文）
CREATE INDEX idx_wiki_page_search_vector ON wiki_page USING gin(search_vector);

-- 三元组索引用于标题模糊搜索
CREATE INDEX idx_wiki_page_title_trgm ON wiki_page USING gin(title gin_trgm_ops);

-- 自动更新 updated_at 字段的触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_wiki_page_updated_at
    BEFORE UPDATE ON wiki_page
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 自动更新全文搜索向量的触发器
CREATE OR REPLACE FUNCTION update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', COALESCE(NEW.content, '')), 'B') ||
        setweight(to_tsvector('simple', COALESCE(NEW.content_summary, '')), 'C');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_wiki_page_search_vector
    BEFORE INSERT OR UPDATE OF title, content, content_summary ON wiki_page
    FOR EACH ROW
EXECUTE FUNCTION update_search_vector();

-- =====================================================
-- 2. Wiki版本历史归档表（超过10个版本后归档）
-- =====================================================
CREATE TABLE IF NOT EXISTS wiki_version_history (
                                                    id BIGINT PRIMARY KEY,
                                                    wiki_page_id BIGINT NOT NULL,
                                                    project_id BIGINT NOT NULL,
                                                    version INTEGER NOT NULL,
                                                    content_diff TEXT,
                                                    change_description VARCHAR(500),
                                                    editor_id BIGINT NOT NULL,
                                                    created_at TIMESTAMP NOT NULL,
                                                    added_lines INTEGER,
                                                    deleted_lines INTEGER,
                                                    changed_chars INTEGER,
                                                    content_hash VARCHAR(64),
                                                    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                                    CONSTRAINT fk_wiki_page FOREIGN KEY (wiki_page_id) REFERENCES wiki_page(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_wiki_version_history_wiki_version ON wiki_version_history(wiki_page_id, version DESC);
CREATE INDEX idx_wiki_version_history_project_created ON wiki_version_history(project_id, created_at DESC);
CREATE INDEX idx_wiki_version_history_created_at ON wiki_version_history(created_at DESC);

-- 分区表（按project_id进行范围分区，提高大数据量查询性能）
-- 注意：需要根据实际项目ID范围创建分区
-- ALTER TABLE wiki_version_history PARTITION BY RANGE (project_id);

-- =====================================================
-- 3. Wiki附件表
-- =====================================================
CREATE TABLE IF NOT EXISTS wiki_attachment (
                                               id BIGINT PRIMARY KEY,
                                               wiki_page_id BIGINT NOT NULL,
                                               project_id BIGINT NOT NULL,
                                               attachment_type VARCHAR(20) NOT NULL,
                                               file_name VARCHAR(255) NOT NULL,
                                               file_size BIGINT NOT NULL,
                                               file_type VARCHAR(50),
                                               mime_type VARCHAR(100),
                                               bucket_name VARCHAR(100) NOT NULL,
                                               object_key VARCHAR(500) NOT NULL,
                                               file_url VARCHAR(1000) NOT NULL,
                                               description VARCHAR(500),
                                               file_hash VARCHAR(32),
                                               upload_by BIGINT NOT NULL,
                                               upload_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               is_deleted BOOLEAN DEFAULT FALSE,
                                               deleted_at TIMESTAMP,
                                               deleted_by BIGINT,
                                               reference_count INTEGER DEFAULT 0,
                                               metadata JSONB DEFAULT '{}'::jsonb,
                                               thumbnail_url VARCHAR(1000),

                                               CONSTRAINT fk_wiki_page_attachment FOREIGN KEY (wiki_page_id) REFERENCES wiki_page(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_wiki_attachment_wiki_page ON wiki_attachment(wiki_page_id);
CREATE INDEX idx_wiki_attachment_project ON wiki_attachment(project_id);
CREATE INDEX idx_wiki_attachment_type ON wiki_attachment(attachment_type);
CREATE INDEX idx_wiki_attachment_upload_by ON wiki_attachment(upload_by);
CREATE INDEX idx_wiki_attachment_upload_at ON wiki_attachment(upload_at DESC);
CREATE INDEX idx_wiki_attachment_deleted ON wiki_attachment(is_deleted) WHERE is_deleted = FALSE;
CREATE INDEX idx_wiki_attachment_file_hash ON wiki_attachment(file_hash);

-- GIN索引用于metadata JSONB查询
CREATE INDEX idx_wiki_attachment_metadata ON wiki_attachment USING gin(metadata);

-- =====================================================
-- 4. 实用查询函数
-- =====================================================

-- 获取Wiki页面的完整内容（根据版本号重建）
CREATE OR REPLACE FUNCTION get_wiki_content_by_version(
    p_wiki_page_id BIGINT,
    p_version INTEGER
)
RETURNS TEXT AS $$
DECLARE
    v_content TEXT;
v_current_version INTEGER;
v_recent_version JSONB;
v_history_record RECORD;
BEGIN
-- 获取当前版本号
SELECT current_version, content INTO v_current_version, v_content
FROM wiki_page
WHERE id = p_wiki_page_id;

-- 如果请求当前版本，直接返回
IF p_version = v_current_version THEN
        RETURN v_content;
END IF;

    -- 如果请求的版本在recent_versions中
SELECT value INTO v_recent_version
FROM wiki_page,
     jsonb_array_elements(recent_versions) AS version
WHERE id = p_wiki_page_id
  AND (value->>'version')::INTEGER = p_version;

IF FOUND THEN
        -- 从最新版本开始应用反向补丁
        -- 这里需要实现diff的反向应用逻辑
        -- 实际实现需要根据使用的diff库来完成
        RETURN 'Content reconstruction from recent versions';
END IF;

    -- 如果版本在归档表中
SELECT content_diff INTO v_history_record
FROM wiki_version_history
WHERE wiki_page_id = p_wiki_page_id
  AND version = p_version;

IF FOUND THEN
        -- 从归档版本重建内容
        RETURN 'Content reconstruction from archived versions';
END IF;

RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 搜索Wiki页面（全文搜索）
CREATE OR REPLACE FUNCTION search_wiki_pages(
    p_project_id BIGINT,
    p_query TEXT,
    p_limit INTEGER DEFAULT 20,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE (
    id BIGINT,
    title VARCHAR,
    content_summary VARCHAR,
    rank REAL
) AS $$
BEGIN
    RETURN QUERY
SELECT
    wp.id,
    wp.title,
    wp.content_summary,
    ts_rank(wp.search_vector, plainto_tsquery('simple', p_query)) AS rank
FROM wiki_page wp
WHERE wp.project_id = p_project_id
  AND wp.search_vector @@ plainto_tsquery('simple', p_query)
ORDER BY rank DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- 获取Wiki页面树（递归查询）
CREATE OR REPLACE FUNCTION get_wiki_tree(
    p_project_id BIGINT,
    p_parent_id BIGINT DEFAULT NULL
)
RETURNS TABLE (
    id BIGINT,
    parent_id BIGINT,
    title VARCHAR,
    page_type VARCHAR,
    level INTEGER,
    path VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE wiki_tree AS (
        -- 根节点
        SELECT
            wp.id,
            wp.parent_id,
            wp.title,
            wp.page_type::VARCHAR,
            1 AS level,
            wp.path
        FROM wiki_page wp
        WHERE wp.project_id = p_project_id
          AND (p_parent_id IS NULL AND wp.parent_id IS NULL
               OR wp.parent_id = p_parent_id)

        UNION ALL

        -- 递归子节点
        SELECT
            wp.id,
            wp.parent_id,
            wp.title,
            wp.page_type::VARCHAR,
            wt.level + 1,
            wp.path
        FROM wiki_page wp
        INNER JOIN wiki_tree wt ON wp.parent_id = wt.id
        WHERE wp.project_id = p_project_id
    )
SELECT * FROM wiki_tree
ORDER BY path, sort_order;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 5. 注释
-- =====================================================
COMMENT ON TABLE wiki_page IS 'Wiki页面表，存储元数据、当前内容和最近10个版本';
COMMENT ON TABLE wiki_version_history IS 'Wiki版本历史归档表，存储超过10个版本后的历史记录';
COMMENT ON TABLE wiki_attachment IS 'Wiki附件表，存储图片和文件元数据';

COMMENT ON COLUMN wiki_page.recent_versions IS 'JSONB格式存储最近10个版本的差异补丁';
COMMENT ON COLUMN wiki_page.search_vector IS 'tsvector类型，用于全文搜索';
COMMENT ON COLUMN wiki_page.active_editors IS 'BIGINT数组，存储当前在线编辑者ID列表';
COMMENT ON COLUMN wiki_attachment.metadata IS 'JSONB格式存储扩展元数据（如图片尺寸、视频时长等）';