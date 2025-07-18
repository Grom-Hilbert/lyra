package tslc.beihaiyun.lyra.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import tslc.beihaiyun.lyra.entity.User;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自定义存储库测试
 * 测试CustomRepository接口的功能
 */
@DataJpaTest
@ActiveProfiles("test")
class CustomRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;

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

        testUser3 = new User();
        testUser3.setUsername("adminuser");
        testUser3.setEmail("admin@example.com");
        testUser3.setDisplayName("Admin User");
        testUser3.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        testUser3.setStatus(User.UserStatus.ACTIVE);
        testUser3.setAuthProvider(User.AuthProvider.OAUTH2);

        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
        entityManager.persistAndFlush(testUser3);
    }

    @Test
    void testBatchSave() {
        // Given
        User newUser1 = new User();
        newUser1.setUsername("batchuser1");
        newUser1.setEmail("batch1@example.com");
        newUser1.setDisplayName("Batch User 1");
        newUser1.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        newUser1.setStatus(User.UserStatus.ACTIVE);
        newUser1.setAuthProvider(User.AuthProvider.LOCAL);

        User newUser2 = new User();
        newUser2.setUsername("batchuser2");
        newUser2.setEmail("batch2@example.com");
        newUser2.setDisplayName("Batch User 2");
        newUser2.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        newUser2.setStatus(User.UserStatus.ACTIVE);
        newUser2.setAuthProvider(User.AuthProvider.LOCAL);

        List<User> usersToSave = Arrays.asList(newUser1, newUser2);

        // When
        List<User> savedUsers = userRepository.batchSave(usersToSave);

        // Then
        assertThat(savedUsers).hasSize(2);
        assertThat(savedUsers).allMatch(user -> user.getId() != null);
        
        // 验证数据库中的数据
        entityManager.flush();
        entityManager.clear();
        
        long totalCount = userRepository.count();
        assertThat(totalCount).isEqualTo(5); // 3个初始用户 + 2个批量保存的用户
    }

    @Test
    void testBatchUpdate() {
        // Given
        testUser1.setEmail("updated1@example.com");
        testUser2.setEmail("updated2@example.com");
        List<User> usersToUpdate = Arrays.asList(testUser1, testUser2);

        // When
        List<User> updatedUsers = userRepository.batchUpdate(usersToUpdate);

        // Then
        assertThat(updatedUsers).hasSize(2);
        
        // 验证更新
        entityManager.flush();
        entityManager.clear();
        
        User updatedUser1 = userRepository.findById(testUser1.getId()).orElse(null);
        User updatedUser2 = userRepository.findById(testUser2.getId()).orElse(null);
        
        assertThat(updatedUser1).isNotNull();
        assertThat(updatedUser1.getEmail()).isEqualTo("updated1@example.com");
        assertThat(updatedUser2).isNotNull();
        assertThat(updatedUser2.getEmail()).isEqualTo("updated2@example.com");
    }

    // 注意：由于简化了CustomRepository接口，这些测试方法已被移除
    // 如果需要Example查询功能，可以直接使用JpaRepository提供的方法
}