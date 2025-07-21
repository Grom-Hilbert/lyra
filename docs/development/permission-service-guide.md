# 权限服务使用指南

## 概述

Lyra 权限服务组件提供了全面的权限检查和控制机制，支持基于角色的权限控制（RBAC）、资源级权限控制以及权限继承逻辑。

## 核心组件

### 1. PermissionService

权限服务是权限系统的核心，提供以下功能：

- 权限检查和验证
- 权限继承逻辑处理
- 资源级权限控制
- 权限缓存管理

#### 主要方法

```java
// 检查用户是否有特定权限
boolean hasPermission(Long userId, String permissionCode)

// 检查用户对特定资源的权限
boolean hasResourcePermission(Long userId, String resourceType, Long resourceId, String action)

// 获取用户的所有权限
Set<String> getUserPermissions(Long userId)

// 获取用户的有效角色
Set<String> getUserActiveRoles(Long userId)

// 检查空间权限
boolean hasSpacePermission(Long userId, Long spaceId, String action)
```

### 2. 权限注解系统

使用 `@RequiresPermission` 注解实现方法级权限控制：

#### 基础用法

```java
@RequiresPermission("FILE_READ")
public FileEntity getFile(Long fileId) {
    // 只有拥有 FILE_READ 权限的用户才能执行此方法
}
```

#### 多权限检查

```java
// AND 逻辑：需要同时拥有所有权限
@RequiresPermission(value = {"FILE_READ", "FILE_WRITE"}, logical = Logical.AND)
public void updateFile(Long fileId, String content) {
    // 用户必须同时拥有 FILE_READ 和 FILE_WRITE 权限
}

// OR 逻辑：拥有任意一个权限即可
@RequiresPermission(value = {"ADMIN", "FILE_MANAGER"}, logical = Logical.OR)
public void manageFile(Long fileId) {
    // 用户拥有 ADMIN 或 FILE_MANAGER 权限即可
}
```

#### 资源级权限检查

```java
@RequiresPermission(
    value = "FILE_READ",
    checkResource = true,
    resourceType = "file",
    resourceIdParam = "fileId"
)
public FileEntity getFileWithResourceCheck(Long fileId) {
    // 检查用户对特定文件的读取权限
}
```

### 3. 缓存机制

权限服务使用 Spring Cache 提供性能优化：

- **用户权限缓存** (`userPermissions`): 缓存用户的所有权限
- **用户角色缓存** (`userRoles`): 缓存用户的活跃角色
- **权限检查缓存** (`permissionCheck`): 缓存具体的权限检查结果
- **继承权限缓存** (`inheritedPermissions`): 缓存权限继承结果

#### 缓存失效

```java
// 清除用户的所有权限缓存
@CacheEvict(value = {"userPermissions", "userRoles", "permissionCheck"}, key = "#userId")
public void clearUserPermissionCache(Long userId)

// 清除所有权限相关缓存
@CacheEvict(value = {"userPermissions", "userRoles", "permissionCheck", "inheritedPermissions"}, allEntries = true)
public void clearAllPermissionCache()
```

## 使用示例

### 1. 在 Service 层使用权限检查

```java
@Service
public class FileService {
    
    @Autowired
    private PermissionService permissionService;
    
    public FileEntity getFile(Long fileId, Long userId) {
        // 检查基础读取权限
        if (!permissionService.hasPermission(userId, "FILE_READ")) {
            throw new AccessDeniedException("没有文件读取权限");
        }
        
        // 检查对特定文件的权限
        if (!permissionService.hasResourcePermission(userId, "file", fileId, "read")) {
            throw new AccessDeniedException("没有访问此文件的权限");
        }
        
        return fileRepository.findById(fileId).orElse(null);
    }
}
```

### 2. 在 Controller 层使用注解

```java
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @GetMapping("/{fileId}")
    @RequiresPermission(
        value = "FILE_READ",
        checkResource = true,
        resourceType = "file",
        resourceIdParam = "fileId"
    )
    public ResponseEntity<FileEntity> getFile(@PathVariable Long fileId) {
        // 权限检查由注解自动处理
        return ResponseEntity.ok(fileService.getFile(fileId));
    }
}
```

### 3. 空间权限检查

```java
@RequiresPermission("SPACE_ACCESS")
public void accessSpace(Long spaceId, Long userId) {
    // 检查用户对空间的访问权限
    if (!permissionService.hasSpacePermission(userId, spaceId, "access")) {
        throw new AccessDeniedException("没有访问此空间的权限");
    }
    
    // 执行空间相关操作
}
```

## 权限继承规则

1. **角色权限继承**: 用户通过角色获得权限，角色权限会自动继承给用户
2. **空间权限继承**: 空间权限可以从父空间继承，子空间自动获得父空间的权限
3. **权限覆盖**: 直接分配给用户的权限会覆盖从角色继承的权限
4. **权限级别**: 权限具有级别概念，高级别权限包含低级别权限的所有功能

## 性能优化建议

1. **合理使用缓存**: 权限检查结果会被缓存，避免重复的数据库查询
2. **批量权限检查**: 对于需要检查多个权限的场景，使用批量检查方法
3. **权限预加载**: 在用户登录时预加载权限信息到缓存
4. **定期清理缓存**: 在权限变更时及时清理相关缓存

## 最佳实践

1. **权限粒度**: 合理设计权限粒度，既要满足安全需求，又要保持系统的可维护性
2. **权限命名**: 使用清晰、一致的权限命名规范，如 `RESOURCE_ACTION` 格式
3. **错误处理**: 权限检查失败时提供清晰的错误信息
4. **审计日志**: 记录重要的权限操作和权限变更
5. **测试覆盖**: 确保权限相关功能有充分的测试覆盖

## 故障排查

### 常见问题

1. **权限检查失败**: 检查用户是否有对应的角色和权限分配
2. **缓存问题**: 权限变更后没有及时清理缓存
3. **注解不生效**: 确保方法是通过 Spring 容器调用的，而不是直接调用
4. **资源权限检查失败**: 检查资源参数名称和类型是否正确

### 调试技巧

1. 启用权限服务的调试日志：

```properties
logging.level.tslc.beihaiyun.lyra.service.PermissionService=DEBUG
logging.level.tslc.beihaiyun.lyra.security.interceptor.PermissionInterceptor=DEBUG
```

2. 使用权限服务的调试方法查看用户权限状态

3. 检查缓存内容确认权限数据是否正确加载
