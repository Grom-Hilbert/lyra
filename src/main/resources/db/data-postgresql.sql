-- ===========================================
-- Lyra 企业级文档管理系统 - PostgreSQL初始数据
-- ===========================================

-- 插入基础角色
INSERT INTO roles (code, name, description, type, is_system, enabled, sort_order, created_at, updated_at, created_by, updated_by, is_deleted) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限', 'SYSTEM_ADMIN', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('USER', '普通用户', '普通用户，拥有基础权限', 'USER', true, true, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('GUEST', '访客用户', '访客用户，只读权限', 'GUEST', true, true, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SPACE_ADMIN', '空间管理员', '空间管理员，管理特定空间', 'CUSTOM', false, true, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SPACE_USER', '空间用户', '空间用户，访问特定空间', 'CUSTOM', false, true, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- 插入基础权限
INSERT INTO permissions (code, name, description, resource_type, category, level, is_system, is_enabled, permission_group, created_at, updated_at, created_by, updated_by, is_deleted) VALUES
-- 系统权限
('SYSTEM_ADMIN', '系统管理', '系统管理权限', 'SYSTEM', 'ADMIN', 100, true, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SYSTEM_CONFIG', '系统配置', '系统配置权限', 'SYSTEM', 'ADMIN', 90, true, true, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('USER_MANAGE', '用户管理', '用户管理权限', 'SYSTEM', 'ADMIN', 80, true, true, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('ROLE_MANAGE', '角色管理', '角色管理权限', 'SYSTEM', 'ADMIN', 80, true, true, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),

-- 空间权限
('SPACE_CREATE', '创建空间', '创建新空间权限', 'SPACE', 'WRITE', 70, true, true, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SPACE_READ', '查看空间', '查看空间权限', 'SPACE', 'READ', 30, true, true, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SPACE_WRITE', '编辑空间', '编辑空间权限', 'SPACE', 'WRITE', 60, true, true, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SPACE_DELETE', '删除空间', '删除空间权限', 'SPACE', 'DELETE', 80, true, true, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('SPACE_ADMIN', '空间管理', '空间管理权限', 'SPACE', 'ADMIN', 90, true, true, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),

-- 文件夹权限
('FOLDER_CREATE', '创建文件夹', '创建文件夹权限', 'FOLDER', 'WRITE', 50, true, true, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FOLDER_READ', '查看文件夹', '查看文件夹权限', 'FOLDER', 'READ', 20, true, true, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FOLDER_WRITE', '编辑文件夹', '编辑文件夹权限', 'FOLDER', 'WRITE', 40, true, true, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FOLDER_DELETE', '删除文件夹', '删除文件夹权限', 'FOLDER', 'DELETE', 60, true, true, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),

-- 文件权限
('FILE_UPLOAD', '上传文件', '上传文件权限', 'FILE', 'WRITE', 40, true, true, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FILE_READ', '查看文件', '查看文件权限', 'FILE', 'READ', 10, true, true, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FILE_DOWNLOAD', '下载文件', '下载文件权限', 'FILE', 'READ', 20, true, true, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FILE_WRITE', '编辑文件', '编辑文件权限', 'FILE', 'WRITE', 30, true, true, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FILE_DELETE', '删除文件', '删除文件权限', 'FILE', 'DELETE', 50, true, true, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('FILE_SHARE', '分享文件', '分享文件权限', 'FILE', 'SHARE', 40, true, true, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),

-- 版本控制权限
('VERSION_READ', '查看版本', '查看文件版本权限', 'FILE', 'READ', 25, true, true, 'VERSION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('VERSION_CREATE', '创建版本', '创建文件版本权限', 'FILE', 'WRITE', 35, true, true, 'VERSION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false),
('VERSION_DELETE', '删除版本', '删除文件版本权限', 'FILE', 'DELETE', 45, true, true, 'VERSION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', false)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- 插入系统配置
INSERT INTO system_config (config_key, config_value, data_type, description) VALUES
-- 基础系统配置
('system.name', 'Lyra 企业级文档管理系统', 'STRING', '系统名称'),
('system.version', '1.0.0-SNAPSHOT', 'STRING', '系统版本'),
('system.max_users', '100', 'NUMBER', '最大用户数量'),
('system.maintenance_mode', 'false', 'BOOLEAN', '维护模式'),

-- 文件系统配置
('storage.default_quota', '10737418240', 'NUMBER', '默认用户配额(字节)'),
('storage.max_file_size', '104857600', 'NUMBER', '单文件最大大小(字节)'),
('storage.allowed_extensions', '["txt","pdf","doc","docx","xls","xlsx","ppt","pptx","jpg","jpeg","png","gif","mp4","mp3","zip","rar"]', 'JSON', '允许的文件扩展名'),
('storage.enable_deduplication', 'true', 'BOOLEAN', '启用文件去重'),

-- 安全配置
('security.password_min_length', '8', 'NUMBER', '密码最小长度'),
('security.password_require_special', 'true', 'BOOLEAN', '密码需要特殊字符'),
('security.login_max_attempts', '5', 'NUMBER', '最大登录尝试次数'),
('security.session_timeout', '3600', 'NUMBER', '会话超时时间(秒)'),

-- WebDAV配置
('webdav.enabled', 'true', 'BOOLEAN', '启用WebDAV'),
('webdav.readonly', 'false', 'BOOLEAN', 'WebDAV只读模式'),

-- 版本控制配置
('version.enabled', 'true', 'BOOLEAN', '启用版本控制'),
('version.max_versions', '10', 'NUMBER', '最大版本数量'),
('version.auto_cleanup', 'true', 'BOOLEAN', '自动清理旧版本'),

-- 分享配置
('share.enabled', 'true', 'BOOLEAN', '启用文件分享'),
('share.default_expiry_days', '7', 'NUMBER', '默认分享过期天数'),
('share.max_download_count', '100', 'NUMBER', '最大下载次数'),

-- 搜索配置
('search.enabled', 'true', 'BOOLEAN', '启用搜索功能'),
('search.index_content', 'false', 'BOOLEAN', '索引文件内容'),
('search.max_results', '100', 'NUMBER', '最大搜索结果数'),

-- 缓存配置
('cache.enabled', 'true', 'BOOLEAN', '启用缓存'),
('cache.ttl', '3600000', 'NUMBER', '缓存TTL(毫秒)'),
('cache.max_size', '1000', 'NUMBER', '最大缓存项数'),

-- 审计配置
('audit.enabled', 'true', 'BOOLEAN', '启用审计日志'),
('audit.retention_days', '365', 'NUMBER', '审计日志保留天数'),

-- 通知配置
('notification.email_enabled', 'false', 'BOOLEAN', '启用邮件通知'),
('notification.webhook_enabled', 'false', 'BOOLEAN', '启用Webhook通知')
ON CONFLICT (config_key) DO UPDATE SET 
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;
