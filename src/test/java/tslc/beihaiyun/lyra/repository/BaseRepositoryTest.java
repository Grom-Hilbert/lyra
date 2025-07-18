package tslc.beihaiyun.lyra.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import tslc.beihaiyun.lyra.entity.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础存储库测试
 * 测试BaseRepository接口的通用功能
 */
@DataJpaTest
@ActiveProfiles("test")
class BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@example.com");
        testUser1.setDisplayName("Test User 1");
        testUser1.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        testUser1.setStatus(User.UserStatus.ACTIVE);
        testUser1.setAuthProvider(User.AuthProvider.LOCAL);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@example.com");
        testUser2.setDisplayName("Test User 2");
        testUser2.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        testUser2.setStatus(User.UserStatus.PENDING);
        testUser2.setAuthProvider(User.AuthProvider.LOCAL);

        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
    }

    @Test
    void testFindByIdOrThrow_WhenExists() {
        // Given
        Long userId = testUser1.getId();

        // When
        User foundUser = userRepository.findByIdOrThrow(userId);

        // Then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("testuser1");
    }

    @Test
    void testFindByIdOrThrow_WhenNotExists() {
        // Given
        Long nonExistentId = 999L;

        // When & Then
        // 注意：实际抛出的异常可能被Spring包装
        assertThrows(Exception.class, 
                    () -> userRepository.findByIdOrThrow(nonExistentId));
    }

    @Test
    void testFindByIds() {
        // Given
        List<Long> userIds = Arrays.asList(testUser1.getId(), testUser2.getId());

        // When
        List<User> foundUsers = userRepository.findByIds(userIds);

        // Then
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting(User::getUsername)
                              .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    void testSoftDeleteById() {
        // Given
        Long userId = testUser1.getId();
        LocalDateTime deletedAt = LocalDateTime.now();

        // When - User实体不支持软删除，所以返回0
        int updatedCount = userRepository.softDeleteById(userId, deletedAt);

        // Then
        assertThat(updatedCount).isEqualTo(0);
        
        // 验证用户仍然存在
        Optional<User> user = userRepository.findById(userId);
        assertThat(user).isPresent();
    }

    @Test
    void testSoftDeleteByIds() {
        // Given
        List<Long> userIds = Arrays.asList(testUser1.getId(), testUser2.getId());
        LocalDateTime deletedAt = LocalDateTime.now();

        // When - User实体不支持软删除，所以返回0
        int updatedCount = userRepository.softDeleteByIds(userIds, deletedAt);

        // Then
        assertThat(updatedCount).isEqualTo(0);
        
        // 验证用户仍然存在
        List<User> users = userRepository.findByIds(userIds);
        assertThat(users).hasSize(2);
    }

    @Test
    void testFindAllNotDeleted() {
        // Given - User实体不支持软删除，所以这个方法返回所有用户

        // When
        List<User> notDeletedUsers = userRepository.findAllNotDeleted();

        // Then
        assertThat(notDeletedUsers).hasSize(2);
        assertThat(notDeletedUsers).extracting(User::getUsername)
                                  .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    void testFindAllNotDeletedWithPagination() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        Page<User> notDeletedUsers = userRepository.findAllNotDeleted(pageRequest);

        // Then
        assertThat(notDeletedUsers.getContent()).hasSize(2);
        assertThat(notDeletedUsers.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testCountNotDeleted() {
        // Given - User实体不支持软删除

        // When
        long count = userRepository.countNotDeleted();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testExistsByIdAndNotDeleted() {
        // Given
        Long userId = testUser1.getId();

        // When - User实体不支持软删除，所以这个方法等同于existsById
        boolean exists = userRepository.existsByIdAndNotDeleted(userId);
        boolean notExists = userRepository.existsByIdAndNotDeleted(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // 注意：由于不是所有实体都有createdAt和updatedAt字段，
    // 这些通用查询方法已从BaseRepository中移除
}