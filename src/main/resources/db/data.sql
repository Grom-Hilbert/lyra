-- ===========================================
-- Lyra 企业级文档管理系统 - 初始化数据
-- ===========================================

-- 插入基础角色
INSERT OR REPLACE INTO roles (code, name, description, type, is_system, enabled, sort_order, created_at, updated_at, created_by, updated_by, is_deleted) VALUES
('ADMIN', '系统管理员', '系统管理员，拥有所有权限', 'SYSTEM_ADMIN', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('USER', '普通用户', '普通用户，拥有基础权限', 'USER', 1, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('GUEST', '访客用户', '访客用户，只读权限', 'GUEST', 1, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('SPACE_ADMIN', '空间管理员', '空间管理员，管理特定空间', 'CUSTOM', 0, 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('SPACE_USER', '空间用户', '空间用户，访问特定空间', 'CUSTOM', 0, 1, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0);

-- 插入基础权限
INSERT OR REPLACE INTO permissions (code, name, description, resource_type, category, level, is_system, is_enabled, permission_group, created_at, updated_at, created_by, updated_by, is_deleted) VALUES
-- 系统权限
('system.admin', '系统管理', '系统管理权限', 'SYSTEM', 'ADMIN', 90, 1, 1, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('system.config', '系统配置', '系统配置权限', 'SYSTEM', 'ADMIN', 80, 1, 1, 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('user.manage', '用户管理', '用户管理权限', 'SYSTEM', 'ADMIN', 70, 1, 1, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('role.manage', '角色管理', '角色管理权限', 'SYSTEM', 'ADMIN', 70, 1, 1, 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('audit.read', '审计查看', '查看审计日志权限', 'SYSTEM', 'READ', 60, 1, 1, 'AUDIT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),

-- 空间权限
('space.create', '创建空间', '创建新空间权限', 'SPACE', 'WRITE', 50, 1, 1, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('space.read', '访问空间', '访问空间权限', 'SPACE', 'READ', 30, 1, 1, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('space.write', '修改空间', '修改空间权限', 'SPACE', 'WRITE', 40, 1, 1, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('space.delete', '删除空间', '删除空间权限', 'SPACE', 'DELETE', 60, 1, 1, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('space.admin', '管理空间', '管理空间权限', 'SPACE', 'ADMIN', 70, 1, 1, 'SPACE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),

-- 文件夹权限
('folder.create', '创建文件夹', '创建文件夹权限', 'FOLDER', 'WRITE', 50, 1, 1, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('folder.read', '访问文件夹', '访问文件夹权限', 'FOLDER', 'READ', 30, 1, 1, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('folder.write', '修改文件夹', '修改文件夹权限', 'FOLDER', 'WRITE', 40, 1, 1, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('folder.delete', '删除文件夹', '删除文件夹权限', 'FOLDER', 'DELETE', 60, 1, 1, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('folder.admin', '管理文件夹', '管理文件夹权限', 'FOLDER', 'ADMIN', 70, 1, 1, 'FOLDER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),

-- 文件权限
('file.upload', '上传文件', '上传文件权限', 'FILE', 'WRITE', 50, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('file.read', '读取文件', '读取文件权限', 'FILE', 'READ', 30, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('file.download', '下载文件', '下载文件权限', 'FILE', 'READ', 35, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('file.write', '修改文件', '修改文件权限', 'FILE', 'WRITE', 40, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('file.delete', '删除文件', '删除文件权限', 'FILE', 'DELETE', 60, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('file.share', '分享文件', '分享文件权限', 'FILE', 'SHARE', 45, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0),
('file.version', '版本管理', '文件版本管理权限', 'FILE', 'WRITE', 50, 1, 1, 'FILE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'system', 'system', 0);

-- 注意：角色权限关联由@ManyToMany注解自动管理，不需要手动插入数据
-- 权限分配将通过应用程序代码在首次启动时自动完成

-- 注意：管理员用户、角色分配、个人空间和根文件夹的创建
-- 现在由 DatabaseInitializationConfig.createAdminUser() 方法动态处理
-- 支持通过配置文件和环境变量自定义管理员信息

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