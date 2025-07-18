package tslc.beihaiyun.lyra.service;

import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 定义用户管理的核心业务逻辑
 */
public interface UserService {

    /**
     * 创建新用户
     */
    User createUser(User user);

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 更新用户信息
     */
    User updateUser(User user);

    /**
     * 删除用户
     */
    void deleteUser(Long id);

    /**
     * 激活用户
     */
    User activateUser(Long id);

    /**
     * 暂停用户
     */
    User suspendUser(Long id);

    /**
     * 为用户分配角色
     */
    User assignRole(Long userId, Long roleId);

    /**
     * 移除用户角色
     */
    User removeRole(Long userId, Long roleId);

    /**
     * 获取用户的所有角色
     */
    List<Role> getUserRoles(Long userId);

    /**
     * 获取待审批的用户列表
     */
    List<User> getPendingUsers();

    /**
     * 审批用户注册
     */
    User approveUser(Long userId);

    /**
     * 拒绝用户注册
     */
    void rejectUser(Long userId);

    /**
     * 检查用户名是否可用
     */
    boolean isUsernameAvailable(String username);

    /**
     * 检查邮箱是否可用
     */
    boolean isEmailAvailable(String email);

    /**
     * 更新用户最后登录时间
     */
    void updateLastLogin(Long userId);

    /**
     * 根据认证提供者查找或创建用户
     */
    User findOrCreateByExternalAuth(String externalId, User.AuthProvider provider, String email, String displayName);
}