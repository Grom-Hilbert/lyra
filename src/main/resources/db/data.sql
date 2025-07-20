-- ===========================================
-- Lyra 企业级文档管理系统 - 初始化数据
-- ===========================================

-- 插入基础角色
INSERT OR IGNORE INTO roles (name, display_name, description, is_system) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限', TRUE),
('USER', '普通用户', '普通用户，拥有基础权限', TRUE),
('GUEST', '访客用户', '访客用户，只读权限', TRUE),
('SPACE_ADMIN', '空间管理员', '空间管理员，管理特定空间', FALSE),
('SPACE_USER', '空间用户', '空间用户，访问特定空间', FALSE);

-- 插入基础权限
INSERT OR IGNORE INTO permissions (name, display_name, description, resource_type, action) VALUES
-- 系统权限
('SYSTEM_ADMIN', '系统管理', '系统管理权限', 'SYSTEM', 'ADMIN'),
('SYSTEM_CONFIG', '系统配置', '系统配置权限', 'SYSTEM', 'CONFIG'),
('USER_MANAGE', '用户管理', '用户管理权限', 'USER', 'MANAGE'),
('ROLE_MANAGE', '角色管理', '角色管理权限', 'ROLE', 'MANAGE'),
('AUDIT_VIEW', '审计查看', '查看审计日志权限', 'AUDIT', 'READ'),

-- 空间权限
('SPACE_CREATE', '创建空间', '创建新空间权限', 'SPACE', 'CREATE'),
('SPACE_READ', '访问空间', '访问空间权限', 'SPACE', 'READ'),
('SPACE_WRITE', '修改空间', '修改空间权限', 'SPACE', 'WRITE'),
('SPACE_DELETE', '删除空间', '删除空间权限', 'SPACE', 'DELETE'),
('SPACE_ADMIN', '管理空间', '管理空间权限', 'SPACE', 'ADMIN'),

-- 文件夹权限
('FOLDER_CREATE', '创建文件夹', '创建文件夹权限', 'FOLDER', 'CREATE'),
('FOLDER_READ', '访问文件夹', '访问文件夹权限', 'FOLDER', 'READ'),
('FOLDER_WRITE', '修改文件夹', '修改文件夹权限', 'FOLDER', 'WRITE'),
('FOLDER_DELETE', '删除文件夹', '删除文件夹权限', 'FOLDER', 'DELETE'),
('FOLDER_ADMIN', '管理文件夹', '管理文件夹权限', 'FOLDER', 'ADMIN'),

-- 文件权限
('FILE_UPLOAD', '上传文件', '上传文件权限', 'FILE', 'CREATE'),
('FILE_READ', '读取文件', '读取文件权限', 'FILE', 'READ'),
('FILE_DOWNLOAD', '下载文件', '下载文件权限', 'FILE', 'DOWNLOAD'),
('FILE_WRITE', '修改文件', '修改文件权限', 'FILE', 'WRITE'),
('FILE_DELETE', '删除文件', '删除文件权限', 'FILE', 'DELETE'),
('FILE_SHARE', '分享文件', '分享文件权限', 'FILE', 'SHARE'),
('FILE_VERSION', '版本管理', '文件版本管理权限', 'FILE', 'VERSION');

-- 为ADMIN角色分配所有权限
INSERT OR IGNORE INTO role_permissions (role_id, permission_id, granted_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'ADMIN';

-- 为USER角色分配基础权限
INSERT OR IGNORE INTO role_permissions (role_id, permission_id, granted_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN (
    'SPACE_CREATE', 'SPACE_READ', 'SPACE_WRITE',
    'FOLDER_CREATE', 'FOLDER_READ', 'FOLDER_WRITE', 'FOLDER_DELETE',
    'FILE_UPLOAD', 'FILE_READ', 'FILE_DOWNLOAD', 'FILE_WRITE', 'FILE_DELETE', 'FILE_SHARE', 'FILE_VERSION'
);

-- 为GUEST角色分配只读权限
INSERT OR IGNORE INTO role_permissions (role_id, permission_id, granted_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'GUEST' AND p.name IN (
    'SPACE_READ', 'FOLDER_READ', 'FILE_READ', 'FILE_DOWNLOAD'
);

-- 为SPACE_ADMIN角色分配空间管理权限
INSERT OR IGNORE INTO role_permissions (role_id, permission_id, granted_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'SPACE_ADMIN' AND p.name IN (
    'SPACE_READ', 'SPACE_WRITE', 'SPACE_ADMIN',
    'FOLDER_CREATE', 'FOLDER_READ', 'FOLDER_WRITE', 'FOLDER_DELETE', 'FOLDER_ADMIN',
    'FILE_UPLOAD', 'FILE_READ', 'FILE_DOWNLOAD', 'FILE_WRITE', 'FILE_DELETE', 'FILE_SHARE', 'FILE_VERSION'
);

-- 为SPACE_USER角色分配空间用户权限
INSERT OR IGNORE INTO role_permissions (role_id, permission_id, granted_by)
SELECT r.id, p.id, 'system'
FROM roles r, permissions p
WHERE r.name = 'SPACE_USER' AND p.name IN (
    'SPACE_READ', 'FOLDER_READ', 'FOLDER_CREATE', 'FOLDER_WRITE',
    'FILE_UPLOAD', 'FILE_READ', 'FILE_DOWNLOAD', 'FILE_WRITE', 'FILE_SHARE', 'FILE_VERSION'
);

-- 插入默认管理员用户
INSERT OR IGNORE INTO users (username, email, password_hash, display_name, status, user_type)
VALUES ('admin', 'admin@lyra.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig7cO86yx/Iq', '系统管理员', 'ACTIVE', 'ADMIN');

-- 为默认管理员分配ADMIN角色
INSERT OR IGNORE INTO user_roles (user_id, role_id, granted_by)
SELECT u.id, r.id, 'system'
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';

-- 为默认管理员创建个人空间
INSERT OR IGNORE INTO spaces (name, type, owner_id, description, created_by)
SELECT '管理员个人空间', 'PERSONAL', u.id, '系统管理员的个人文档空间', 'system'
FROM users u
WHERE u.username = 'admin';

-- 为管理员个人空间创建根文件夹
INSERT OR IGNORE INTO folders (name, path, space_id, is_root, created_by)
SELECT '/', '/', s.id, TRUE, 'system'
FROM spaces s, users u
WHERE s.owner_id = u.id AND u.username = 'admin' AND s.type = 'PERSONAL';

-- 插入系统配置
INSERT OR IGNORE INTO system_config (config_key, config_value, data_type, description) VALUES
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
('security.jwt_expiration', '86400000', 'NUMBER', 'JWT过期时间(毫秒)'),
('security.jwt_refresh_expiration', '604800000', 'NUMBER', 'JWT刷新过期时间(毫秒)'),
('security.password_min_length', '6', 'NUMBER', '密码最小长度'),
('security.login_max_attempts', '5', 'NUMBER', '最大登录尝试次数'),
('security.account_lock_duration', '1800000', 'NUMBER', '账户锁定时长(毫秒)'),

-- WebDAV配置
('webdav.enabled', 'true', 'BOOLEAN', '启用WebDAV'),
('webdav.base_path', '/webdav', 'STRING', 'WebDAV基础路径'),
('webdav.digest_auth', 'false', 'BOOLEAN', '启用摘要认证'),

-- 版本控制配置
('version.max_versions', '10', 'NUMBER', '最大版本数量'),
('version.auto_cleanup', 'true', 'BOOLEAN', '自动清理旧版本'),
('version.cleanup_days', '90', 'NUMBER', '版本保留天数'),

-- 缓存配置
('cache.enabled', 'true', 'BOOLEAN', '启用缓存'),
('cache.ttl', '3600000', 'NUMBER', '缓存TTL(毫秒)'),
('cache.max_size', '1000', 'NUMBER', '最大缓存项数'),

-- 审计配置
('audit.enabled', 'true', 'BOOLEAN', '启用审计日志'),
('audit.retention_days', '365', 'NUMBER', '审计日志保留天数'),

-- 通知配置
('notification.email_enabled', 'false', 'BOOLEAN', '启用邮件通知'),
('notification.smtp_host', '', 'STRING', 'SMTP服务器'),
('notification.smtp_port', '587', 'NUMBER', 'SMTP端口'),
('notification.smtp_username', '', 'STRING', 'SMTP用户名'),
('notification.smtp_password', '', 'STRING', 'SMTP密码'),

-- 备份配置
('backup.enabled', 'false', 'BOOLEAN', '启用自动备份'),
('backup.schedule', '0 2 * * *', 'STRING', '备份计划表达式'),
('backup.retention_days', '30', 'NUMBER', '备份保留天数'); 