package tslc.beihaiyun.lyra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.User;

import java.util.Optional;

/**
 * 临时简化的用户Repository
 * 用于排查Repository方法问题
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户 - 基础方法
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户 - 基础方法
     */  
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名查找未删除的用户
     */
    Optional<User> findByUsernameAndDeletedFalse(String username);
    
    /**
     * 根据邮箱查找未删除的用户
     */
    Optional<User> findByEmailAndDeletedFalse(String email);
} 