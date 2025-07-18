package tslc.beihaiyun.lyra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.User;

import java.util.Optional;
import java.util.List;

/**
 * 用户数据访问接口
 * 提供用户相关的数据库操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据外部ID和认证提供者查找用户
     */
    Optional<User> findByExternalIdAndAuthProvider(String externalId, User.AuthProvider authProvider);

    /**
     * 根据用户状态查找用户列表
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据角色查找用户
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * 查找待审批的用户
     */
    @Query("SELECT u FROM User u WHERE u.status = 'PENDING' ORDER BY u.createdAt ASC")
    List<User> findPendingUsers();
}