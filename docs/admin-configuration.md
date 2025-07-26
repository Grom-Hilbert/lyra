# Lyra 系统配置指南

## 概述

Lyra 系统支持通过配置文件和环境变量来自定义数据库连接和初始管理员账户信息，提供了灵活的部署配置选项。

## 数据库配置

### 支持的数据库类型

Lyra 支持以下数据库类型：

- **SQLITE** - 适合开发环境和小型部署，无需额外安装数据库服务
- **MYSQL** - 适合生产环境，性能优秀，广泛使用
- **POSTGRESQL** - 适合企业级部署，功能强大，标准兼容性好

### 数据库配置方式

#### 1. 配置文件方式

在 `application.properties` 中配置：

```properties
# 数据库基本配置
lyra.database.type=SQLITE
lyra.database.host=localhost
lyra.database.port=3306
lyra.database.name=lyra
lyra.database.username=lyra
lyra.database.password=password
lyra.database.file-path=./data/lyra.db

# 连接池配置
lyra.database.max-pool-size=10
lyra.database.min-idle=2
lyra.database.connection-timeout=30000
lyra.database.idle-timeout=600000
lyra.database.max-lifetime=1800000
```

#### 2. 环境变量方式

```bash
# 数据库基本配置
export LYRA_DATABASE_TYPE=MYSQL
export LYRA_DATABASE_HOST=localhost
export LYRA_DATABASE_PORT=3306
export LYRA_DATABASE_NAME=lyra
export LYRA_DATABASE_USERNAME=lyra_user
export LYRA_DATABASE_PASSWORD=secure_password

# 连接池配置（可选）
export LYRA_DATABASE_MAX_POOL_SIZE=20
export LYRA_DATABASE_MIN_IDLE=5
```

### 不同数据库的配置示例

#### SQLite 配置（默认）
```properties
lyra.database.type=SQLITE
lyra.database.file-path=./data/lyra.db
```

#### MySQL 配置
```properties
lyra.database.type=MYSQL
lyra.database.host=localhost
lyra.database.port=3306
lyra.database.name=lyra
lyra.database.username=lyra_user
lyra.database.password=your_password
```

#### PostgreSQL 配置
```properties
lyra.database.type=POSTGRESQL
lyra.database.host=localhost
lyra.database.port=5432
lyra.database.name=lyra
lyra.database.username=lyra_user
lyra.database.password=your_password
```

### 数据库配置参数说明

| 参数 | 环境变量 | 默认值 | 说明 |
|------|----------|--------|------|
| `lyra.database.type` | `LYRA_DATABASE_TYPE` | `SQLITE` | 数据库类型 |
| `lyra.database.host` | `LYRA_DATABASE_HOST` | `localhost` | 数据库主机地址 |
| `lyra.database.port` | `LYRA_DATABASE_PORT` | 自动 | 数据库端口 |
| `lyra.database.name` | `LYRA_DATABASE_NAME` | `lyra` | 数据库名称 |
| `lyra.database.username` | `LYRA_DATABASE_USERNAME` | `lyra` | 数据库用户名 |
| `lyra.database.password` | `LYRA_DATABASE_PASSWORD` | `password` | 数据库密码 |
| `lyra.database.file-path` | `LYRA_DATABASE_FILE_PATH` | `./data/lyra.db` | SQLite文件路径 |
| `lyra.database.max-pool-size` | `LYRA_DATABASE_MAX_POOL_SIZE` | `10` | 最大连接数 |
| `lyra.database.min-idle` | `LYRA_DATABASE_MIN_IDLE` | `2` | 最小空闲连接数 |

## 管理员账户配置

## 配置方式

### 1. 配置文件方式

在 `application.properties` 中配置：

```properties
# 初始管理员账户配置
lyra.admin.username=admin
lyra.admin.password=admin123
lyra.admin.email=admin@lyra.local
lyra.admin.display-name=系统管理员
lyra.admin.storage-quota=10737418240
```

### 2. 环境变量方式

通过环境变量覆盖配置（推荐用于生产环境）：

```bash
# 设置管理员用户名
export LYRA_ADMIN_USERNAME=myadmin

# 设置管理员密码
export LYRA_ADMIN_PASSWORD=MySecurePassword123!

# 设置管理员邮箱
export LYRA_ADMIN_EMAIL=admin@mycompany.com

# 设置管理员显示名称
export LYRA_ADMIN_DISPLAY_NAME="系统管理员"

# 设置存储配额（字节）
export LYRA_ADMIN_STORAGE_QUOTA=21474836480
```

### 3. Docker 环境变量

在 Docker 部署时：

```yaml
version: '3.8'
services:
  lyra:
    image: lyra:latest
    environment:
      - LYRA_ADMIN_USERNAME=admin
      - LYRA_ADMIN_PASSWORD=SecurePassword123!
      - LYRA_ADMIN_EMAIL=admin@company.com
      - LYRA_ADMIN_DISPLAY_NAME=系统管理员
      - LYRA_ADMIN_STORAGE_QUOTA=10737418240
    ports:
      - "8080:8080"
```

## 配置参数说明

| 参数 | 环境变量 | 默认值 | 说明 |
|------|----------|--------|------|
| `lyra.admin.username` | `LYRA_ADMIN_USERNAME` | `admin` | 管理员用户名 |
| `lyra.admin.password` | `LYRA_ADMIN_PASSWORD` | `admin123` | 管理员密码（明文，系统会自动加密） |
| `lyra.admin.email` | `LYRA_ADMIN_EMAIL` | `admin@lyra.local` | 管理员邮箱地址 |
| `lyra.admin.display-name` | `LYRA_ADMIN_DISPLAY_NAME` | `系统管理员` | 管理员显示名称 |
| `lyra.admin.storage-quota` | `LYRA_ADMIN_STORAGE_QUOTA` | `10737418240` | 存储配额（字节，默认10GB） |

## 安全建议

### 1. 密码安全
- **生产环境必须修改默认密码**
- 使用强密码（至少8位，包含大小写字母、数字和特殊字符）
- 定期更换密码

### 2. 环境变量安全
- 生产环境使用环境变量而非配置文件
- 确保环境变量文件权限正确（600）
- 不要在版本控制中提交包含敏感信息的配置文件

### 3. 邮箱配置
- 使用真实的邮箱地址以便接收系统通知
- 确保邮箱地址格式正确

## 存储配额说明

存储配额以字节为单位，常用换算：

- 1GB = 1,073,741,824 字节
- 5GB = 5,368,709,120 字节
- 10GB = 10,737,418,240 字节（默认）
- 20GB = 21,474,836,480 字节
- 50GB = 53,687,091,200 字节

## 初始化流程

1. 系统启动时检查是否存在指定用户名的管理员
2. 如果不存在，使用配置的信息创建管理员用户
3. 自动为管理员分配 ADMIN 角色
4. 创建管理员的个人空间和根文件夹
5. 密码会被自动加密存储

## 故障排除

### 1. 管理员创建失败
- 检查数据库连接是否正常
- 确认配置参数格式正确
- 查看应用日志获取详细错误信息

### 2. 环境变量不生效
- 确认环境变量名称正确（区分大小写）
- 重启应用以加载新的环境变量
- 检查环境变量是否被正确设置

### 3. 密码登录失败
- 确认使用的是配置的明文密码
- 检查用户名是否正确
- 确认账户状态为激活状态

## 示例配置

### 开发环境
```properties
lyra.admin.username=dev-admin
lyra.admin.password=dev123
lyra.admin.email=dev@localhost
lyra.admin.display-name=开发管理员
```

### 生产环境
```bash
export LYRA_ADMIN_USERNAME=prod-admin
export LYRA_ADMIN_PASSWORD=Prod@SecurePass2024!
export LYRA_ADMIN_EMAIL=admin@company.com
export LYRA_ADMIN_DISPLAY_NAME="生产环境管理员"
export LYRA_ADMIN_STORAGE_QUOTA=53687091200
```
