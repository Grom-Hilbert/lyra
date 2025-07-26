-- ===========================================
-- Lyra 企业级文档管理系统 - 初始化数据
-- ===========================================

-- 插入基础角色
INSERT OR IGNORE INTO roles (code, name, description, type, is_system) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限', 'SYSTEM_ADMIN', TRUE),
('USER', '普通用户', '普通用户，拥有基础权限', 'USER', TRUE),
('GUEST', '访客用户', '访客用户，只读权限', 'GUEST', TRUE),
('SPACE_ADMIN', '空间管理员', '空间管理员，管理特定空间', 'CUSTOM', FALSE),
('SPACE_USER', '空间用户', '空间用户，访问特定空间', 'CUSTOM', FALSE);

-- 插入基础权限
INSERT OR IGNORE INTO permissions (code, name, description, resource_type, category, level, is_system, permission_group) VALUES
-- 系统权限
('system.admin', '系统管理', '系统管理权限', 'SYSTEM', 'ADMIN', 90, TRUE, 'SYSTEM'),
('system.config', '系统配置', '系统配置权限', 'SYSTEM', 'ADMIN', 80, TRUE, 'SYSTEM'),
('user.manage', '用户管理', '用户管理权限', 'SYSTEM', 'ADMIN', 70, TRUE, 'USER'),
('role.manage', '角色管理', '角色管理权限', 'SYSTEM', 'ADMIN', 70, TRUE, 'USER'),
('audit.read', '审计查看', '查看审计日志权限', 'SYSTEM', 'READ', 60, TRUE, 'AUDIT'),

-- 空间权限
('space.create', '创建空间', '创建新空间权限', 'SPACE', 'WRITE', 50, TRUE, 'SPACE'),
('space.read', '访问空间', '访问空间权限', 'SPACE', 'READ', 30, TRUE, 'SPACE'),
('space.write', '修改空间', '修改空间权限', 'SPACE', 'WRITE', 40, TRUE, 'SPACE'),
('space.delete', '删除空间', '删除空间权限', 'SPACE', 'DELETE', 60, TRUE, 'SPACE'),
('space.admin', '管理空间', '管理空间权限', 'SPACE', 'ADMIN', 70, TRUE, 'SPACE'),

-- 文件夹权限
('folder.create', '创建文件夹', '创建文件夹权限', 'FOLDER', 'WRITE', 50, TRUE, 'FOLDER'),
('folder.read', '访问文件夹', '访问文件夹权限', 'FOLDER', 'READ', 30, TRUE, 'FOLDER'),
('folder.write', '修改文件夹', '修改文件夹权限', 'FOLDER', 'WRITE', 40, TRUE, 'FOLDER'),
('folder.delete', '删除文件夹', '删除文件夹权限', 'FOLDER', 'DELETE', 60, TRUE, 'FOLDER'),
('folder.admin', '管理文件夹', '管理文件夹权限', 'FOLDER', 'ADMIN', 70, TRUE, 'FOLDER'),

-- 文件权限
('file.upload', '上传文件', '上传文件权限', 'FILE', 'WRITE', 50, TRUE, 'FILE'),
('file.read', '读取文件', '读取文件权限', 'FILE', 'READ', 30, TRUE, 'FILE'),
('file.download', '下载文件', '下载文件权限', 'FILE', 'READ', 35, TRUE, 'FILE'),
('file.write', '修改文件', '修改文件权限', 'FILE', 'WRITE', 40, TRUE, 'FILE'),
('file.delete', '删除文件', '删除文件权限', 'FILE', 'DELETE', 60, TRUE, 'FILE'),
('file.share', '分享文件', '分享文件权限', 'FILE', 'SHARE', 45, TRUE, 'FILE'),
('file.version', '版本管理', '文件版本管理权限', 'FILE', 'WRITE', 50, TRUE, 'FILE');

-- 注意：角色权限关联由@ManyToMany注解自动管理，不需要手动插入数据
-- 权限分配将通过应用程序代码在首次启动时自动完成

-- 插入默认管理员用户
INSERT OR IGNORE INTO users (username, email, password, display_name, status, enabled, email_verified)
VALUES ('admin', 'admin@lyra.local', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lbdOIig7cO86yx/Iq', '系统管理员', 'ACTIVE', TRUE, TRUE);

-- 为默认管理员分配ADMIN角色
INSERT OR IGNORE INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'system'
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN';

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