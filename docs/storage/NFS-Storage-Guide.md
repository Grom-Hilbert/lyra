# NFS存储配置指南

本指南介绍如何在Lyra文档管理系统中配置和使用NFS（Network File System）网络文件系统存储。

## 概述

NFS存储支持允许Lyra将文件存储在远程NFS服务器上，提供以下优势：

- **共享存储**: 多个Lyra实例可以共享同一个存储后端
- **高可用性**: NFS服务器可以配置为高可用集群
- **可扩展性**: 存储容量可以独立于应用服务器进行扩展
- **备份和恢复**: 集中化的存储便于备份和灾难恢复
- **性能**: 专用的存储服务器可以提供更好的I/O性能

## 前提条件

### NFS服务器要求

1. **NFS服务器**: 运行NFS服务的Linux服务器
2. **网络连接**: Lyra服务器与NFS服务器之间的稳定网络连接
3. **权限配置**: 正确的NFS导出权限设置

### 客户端要求

1. **NFS客户端工具**: 安装`nfs-common`包（Ubuntu/Debian）或`nfs-utils`包（CentOS/RHEL）
2. **挂载权限**: 容器需要特权模式或适当的capabilities
3. **网络访问**: 能够访问NFS服务器的2049端口

## 配置步骤

### 1. NFS服务器配置

#### 安装NFS服务器

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nfs-kernel-server

# CentOS/RHEL
sudo yum install nfs-utils
sudo systemctl enable nfs-server
sudo systemctl start nfs-server
```

#### 创建共享目录

```bash
# 创建Lyra存储目录
sudo mkdir -p /exports/lyra-storage
sudo chown nobody:nogroup /exports/lyra-storage
sudo chmod 755 /exports/lyra-storage
```

#### 配置NFS导出

编辑`/etc/exports`文件：

```bash
# 允许特定网络访问
/exports/lyra-storage 192.168.1.0/24(rw,sync,no_subtree_check,no_root_squash)

# 或允许特定主机访问
/exports/lyra-storage 192.168.1.100(rw,sync,no_subtree_check,no_root_squash)
```

应用配置：

```bash
sudo exportfs -ra
sudo systemctl restart nfs-kernel-server
```

#### 验证NFS导出

```bash
# 查看导出列表
sudo exportfs -v

# 从客户端测试
showmount -e NFS_SERVER_IP
```

### 2. Lyra应用配置

#### 配置文件设置

在`application.yml`中配置NFS存储：

```yaml
lyra:
  storage:
    primary: nfs
    nfs:
      enabled: true
      server: 192.168.1.100
      export-path: /exports/lyra-storage
      mount-point: /mnt/nfs-storage
      mount-options: rw,sync,hard,intr
      max-file-size: 104857600  # 100MB
      connection-timeout: 30000  # 30秒
      read-timeout: 60000       # 60秒
      retry-count: 3
```

#### 环境变量配置

```bash
export STORAGE_TYPE=nfs
export NFS_ENABLED=true
export NFS_SERVER=192.168.1.100
export NFS_EXPORT_PATH=/exports/lyra-storage
export NFS_MOUNT_POINT=/mnt/nfs-storage
export NFS_MOUNT_OPTIONS="rw,sync,hard,intr"
```

### 3. Docker部署

#### 使用Docker Compose

```yaml
version: '3.8'
services:
  lyra:
    image: lyra:latest
    privileged: true  # NFS挂载需要特权模式
    environment:
      - STORAGE_TYPE=nfs
      - NFS_ENABLED=true
      - NFS_SERVER=192.168.1.100
      - NFS_EXPORT_PATH=/exports/lyra-storage
      - NFS_MOUNT_POINT=/mnt/nfs-storage
    volumes:
      - /sys/fs/cgroup:/sys/fs/cgroup:ro
```

#### 手动Docker运行

```bash
docker run -d \
  --name lyra-nfs \
  --privileged \
  -p 8080:8080 \
  -e STORAGE_TYPE=nfs \
  -e NFS_ENABLED=true \
  -e NFS_SERVER=192.168.1.100 \
  -e NFS_EXPORT_PATH=/exports/lyra-storage \
  -e NFS_MOUNT_POINT=/mnt/nfs-storage \
  -v /sys/fs/cgroup:/sys/fs/cgroup:ro \
  lyra:latest
```

### 4. Kubernetes部署

#### 创建PersistentVolume

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: lyra-nfs-pv
spec:
  capacity:
    storage: 100Gi
  accessModes:
    - ReadWriteMany
  nfs:
    server: 192.168.1.100
    path: /exports/lyra-storage
  mountOptions:
    - rw
    - sync
    - hard
    - intr
```

#### 部署应用

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: lyra
spec:
  template:
    spec:
      containers:
      - name: lyra
        image: lyra:latest
        env:
        - name: STORAGE_TYPE
          value: "nfs"
        - name: NFS_ENABLED
          value: "true"
        volumeMounts:
        - name: nfs-storage
          mountPath: /mnt/nfs-storage
      volumes:
      - name: nfs-storage
        persistentVolumeClaim:
          claimName: lyra-nfs-pvc
```

## 配置选项详解

### NFS挂载选项

| 选项 | 说明 | 推荐值 |
|------|------|--------|
| `rw` | 读写权限 | 必需 |
| `sync` | 同步写入 | 推荐（数据安全） |
| `async` | 异步写入 | 可选（性能优先） |
| `hard` | 硬挂载 | 推荐（可靠性） |
| `soft` | 软挂载 | 可选（超时处理） |
| `intr` | 可中断 | 推荐 |
| `timeo=600` | 超时时间（0.1秒） | 可选 |
| `retrans=2` | 重传次数 | 可选 |

### 性能调优选项

```yaml
lyra:
  storage:
    nfs:
      mount-options: rw,async,hard,intr,rsize=32768,wsize=32768,timeo=600
      connection-timeout: 60000
      read-timeout: 120000
      retry-count: 5
```

## 监控和故障排除

### 健康检查

Lyra提供NFS存储的健康检查端点：

```bash
# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 检查存储统计信息
curl http://localhost:8080/actuator/health/nfs
```

### 常见问题

#### 1. 挂载失败

**症状**: 应用启动时报告NFS挂载失败

**解决方案**:

- 检查NFS服务器是否运行：`systemctl status nfs-kernel-server`
- 验证网络连接：`telnet NFS_SERVER 2049`
- 检查防火墙设置
- 验证导出配置：`exportfs -v`

#### 2. 权限问题

**症状**: 文件操作失败，权限被拒绝

**解决方案**:

- 检查NFS导出选项中的`no_root_squash`
- 确认共享目录权限：`ls -la /exports/lyra-storage`
- 检查用户映射配置

#### 3. 性能问题

**症状**: 文件操作缓慢

**解决方案**:

- 调整NFS挂载选项（使用`async`而不是`sync`）
- 增加`rsize`和`wsize`参数
- 检查网络延迟和带宽
- 考虑使用NFS v4

#### 4. 连接不稳定

**症状**: 间歇性连接失败

**解决方案**:

- 增加重试次数和超时时间
- 使用`hard`挂载选项
- 检查网络稳定性
- 配置NFS服务器高可用

### 日志分析

#### 应用日志

```bash
# 查看NFS相关日志
docker logs lyra-container | grep -i nfs

# 或在Kubernetes中
kubectl logs deployment/lyra | grep -i nfs
```

#### 系统日志

```bash
# 查看NFS客户端日志
sudo journalctl -u nfs-client
sudo dmesg | grep -i nfs

# 查看NFS服务器日志
sudo journalctl -u nfs-kernel-server
```

## 安全考虑

### 网络安全

1. **防火墙配置**: 限制NFS端口访问
2. **VPN/专网**: 在不安全网络中使用VPN
3. **IP白名单**: 在NFS导出中限制客户端IP

### 数据安全

1. **加密传输**: 使用Kerberos或VPN加密
2. **访问控制**: 配置适当的用户映射
3. **备份策略**: 定期备份NFS存储数据

### 示例安全配置

```bash
# /etc/exports - 安全配置示例
/exports/lyra-storage 192.168.1.0/24(rw,sync,no_subtree_check,root_squash,secure)
```

## 备份和恢复

### 备份策略

```bash
#!/bin/bash
# NFS存储备份脚本
BACKUP_DIR="/backup/lyra-$(date +%Y%m%d)"
NFS_MOUNT="/exports/lyra-storage"

mkdir -p "$BACKUP_DIR"
rsync -av --progress "$NFS_MOUNT/" "$BACKUP_DIR/"
tar -czf "$BACKUP_DIR.tar.gz" -C "$BACKUP_DIR" .
```

### 恢复过程

```bash
#!/bin/bash
# 恢复脚本
BACKUP_FILE="/backup/lyra-20240101.tar.gz"
NFS_MOUNT="/exports/lyra-storage"

# 停止Lyra服务
systemctl stop lyra

# 恢复数据
tar -xzf "$BACKUP_FILE" -C "$NFS_MOUNT"

# 启动Lyra服务
systemctl start lyra
```

## 最佳实践

1. **高可用性**: 配置NFS服务器集群
2. **性能优化**: 根据工作负载调整挂载选项
3. **监控**: 设置NFS性能和可用性监控
4. **备份**: 实施定期备份策略
5. **测试**: 定期测试故障恢复流程
6. **文档**: 维护配置和操作文档

## 参考资料

- [NFS官方文档](https://nfs.sourceforge.net/)
- [Linux NFS配置指南](https://www.kernel.org/doc/Documentation/filesystems/nfs/)
- [Docker NFS最佳实践](https://docs.docker.com/storage/volumes/)
- [Kubernetes NFS存储](https://kubernetes.io/docs/concepts/storage/volumes/#nfs)
