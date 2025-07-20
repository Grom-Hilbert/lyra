package tslc.beihaiyun.lyra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tslc.beihaiyun.lyra.entity.SpacePermission;

import java.util.List;
import java.util.Optional;

/**
 * 空间权限数据访问接口（简化版）
 * 只包含基础查询方法，用于排查问题
 */
@Repository
public interface SpacePermissionRepository extends JpaRepository<SpacePermission, Long> {

    /**
     * 根据用户ID和空间ID查找权限 - 基础方法
     */
    List<SpacePermission> findByUserIdAndSpaceId(Long userId, Long spaceId);

    /**
     * 根据用户ID查找所有空间权限 - 基础方法  
     */
    List<SpacePermission> findByUserId(Long userId);

    /**
     * 根据空间ID查找所有权限 - 基础方法
     */
    List<SpacePermission> findBySpaceId(Long spaceId);

    /**
     * 根据授权状态查找权限 - 基础方法
     */
    List<SpacePermission> findByStatus(String status);
} 