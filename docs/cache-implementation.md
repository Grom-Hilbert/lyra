# Lyra 缓存机制实现文档

## 概述

Lyra 系统实现了完整的缓存机制，支持内存缓存和分布式缓存（Redis），提供缓存预热、监控和管理功能。

## 缓存架构

### 缓存类型

1. **内存缓存（默认）**
   - 使用 ConcurrentMapCacheManager
   - 适用于单机部署
   - 性能最佳，但不支持分布式

2. **Redis缓存**
   - 支持分布式部署
   - 数据持久化
   - 支持集群模式

### 缓存分类

系统定义了以下缓存类别：

- `userPermissions` - 用户权限缓存
- `userRoles` - 用户角色缓存
- `permissionCheck` - 权限检查缓存
- `inheritedPermissions` - 继承权限缓存
- `roleCache` - 角色缓存
- `fileMetadata` - 文件元数据缓存
- `userSession` - 用户会话缓存
- `spaceInfo` - 空间信息缓存
- `folderTree` - 文件夹树缓存
- `systemConfig` - 系统配置缓存

## 配置说明

### 基础配置

```properties
# 缓存类型：memory 或 redis
lyra.cache.type=memory

# 缓存TTL（秒）
lyra.cache.ttl=3600

# 最大缓存条目数
lyra.cache.max-size=10000

# 启用缓存预热
lyra.cache.enable-warmup=true

# 启用缓存统计
lyra.cache.enable-stats=true
```

### 内存缓存配置

```properties
# 初始容量
lyra.cache.memory.initial-capacity=100

# 最大权重
lyra.cache.memory.maximum-weight=100000

# 写入后过期时间（秒）
lyra.cache.memory.expire-after-write=3600

# 访问后过期时间（秒）
lyra.cache.memory.expire-after-access=1800

# 刷新后写入时间（秒）
lyra.cache.memory.refresh-after-write=300
```

### Redis缓存配置

```properties
# Redis主机
lyra.cache.redis.host=localhost

# Redis端口
lyra.cache.redis.port=6379

# Redis密码
lyra.cache.redis.password=

# Redis数据库索引
lyra.cache.redis.database=0

# 连接超时时间（毫秒）
lyra.cache.redis.timeout=5000

# 连接池配置
lyra.cache.redis.max-active=20
lyra.cache.redis.max-idle=10
lyra.cache.redis.min-idle=2
```

## 缓存使用

### 在服务中使用缓存

```java
@Service
public class ExampleService {
    
    // 缓存查询结果
    @Cacheable(value = CacheConfig.FILE_METADATA_CACHE, key = "'file:' + #fileId")
    public Optional<FileEntity> getFileById(Long fileId) {
        return fileRepository.findById(fileId);
    }
    
    // 清理缓存
    @CacheEvict(value = CacheConfig.FILE_METADATA_CACHE, key = "'file:' + #fileId")
    public void updateFile(Long fileId, FileEntity file) {
        fileRepository.save(file);
    }
    
    // 清理所有相关缓存
    @CacheEvict(value = CacheConfig.FILE_METADATA_CACHE, allEntries = true)
    public void clearAllFileCache() {
        // 清理操作
    }
}
```

### 缓存预热

系统在启动时自动执行缓存预热：

1. 预热活跃用户的权限信息
2. 预热启用的角色信息
3. 预热系统配置
4. 预热热门空间信息

可以通过配置禁用：

```properties
lyra.cache.enable-warmup=false
```

## 缓存监控

### 健康检查

缓存健康状态通过 Spring Boot Actuator 暴露：

- 访问 `/actuator/health` 查看整体健康状态
- 缓存相关的健康信息包含在响应中

### 管理接口

系统提供了缓存管理的REST接口：

```bash
# 获取缓存统计信息
GET /api/admin/cache/statistics

# 清理指定缓存
DELETE /api/admin/cache/{cacheName}

# 清理指定缓存的特定键
DELETE /api/admin/cache/{cacheName}/keys/{key}

# 清理所有缓存
DELETE /api/admin/cache/all

# 手动触发缓存预热
POST /api/admin/cache/warmup

# 获取缓存健康状态
GET /api/admin/cache/health
```

### 性能监控

系统通过AOP切面监控缓存操作：

- 记录缓存命中率
- 监控缓存操作耗时
- 统计缓存驱逐次数
- 记录缓存异常

## 最佳实践

### 缓存键设计

1. 使用有意义的前缀：`user:123`, `file:456`
2. 避免键冲突：包含实体类型和ID
3. 考虑键的长度：过长的键影响性能

### 缓存策略

1. **读多写少的数据**：适合缓存
   - 用户权限信息
   - 系统配置
   - 文件元数据

2. **频繁变化的数据**：不适合缓存
   - 实时统计数据
   - 临时状态信息

3. **大对象**：谨慎缓存
   - 考虑内存使用
   - 序列化开销

### 缓存失效策略

1. **时间失效**：设置合理的TTL
2. **主动失效**：数据更新时清理缓存
3. **版本控制**：通过版本号控制缓存一致性

## 故障排除

### 常见问题

1. **缓存未命中**
   - 检查缓存键是否正确
   - 确认缓存配置是否生效
   - 查看缓存统计信息

2. **内存使用过高**
   - 调整最大缓存大小
   - 检查缓存TTL设置
   - 监控缓存驱逐情况

3. **Redis连接问题**
   - 检查Redis服务状态
   - 验证连接配置
   - 查看连接池状态

### 调试技巧

1. 启用缓存调试日志：

```properties
logging.level.org.springframework.cache=DEBUG
logging.level.tslc.beihaiyun.lyra.service.CacheService=DEBUG
```

2. 使用缓存统计接口监控状态
3. 通过健康检查确认缓存可用性

## 性能测试结果

根据性能测试：

- **写入性能**：平均 < 0.1ms/item
- **读取性能**：平均 < 0.05ms/item
- **并发性能**：支持高并发访问
- **内存使用**：合理的内存占用
- **缓存命中率**：> 99%（预热后）

## 扩展计划

1. **Caffeine缓存**：替换ConcurrentMapCacheManager
2. **多级缓存**：L1本地缓存 + L2分布式缓存
3. **缓存预热优化**：智能预热策略
4. **缓存分片**：支持大规模数据缓存
5. **缓存压缩**：减少内存使用
