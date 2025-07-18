package tslc.beihaiyun.lyra.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import tslc.beihaiyun.lyra.entity.Role;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户存储库测试
 * 测试UserRepository的具体功能
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // 创建角色
        adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setType(Role.RoleType.ADMIN);
        adminRole.setDescription("管理员角色");

        userRole = new Role();
        userRole.setName("USER");
        userRole.setType(Role.RoleType.USER);
        userRole.setDescription("普通用户角色");

        entityManager.persistAndFlush(adminRole);
        entityManager.persistAndFlush(userRole);

        // 创建测试用户
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@example.com");
        testUser1.setDisplayName("Test User 1");
        testUser1.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2"); // BCrypt hash for "password"
        testUser1.setStatus(User.UserStatus.ACTIVE);
        testUser1.setAuthProvider(User.AuthProvider.LOCAL);
        testUser1.setRoles(Set.of(userRole));

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@example.com");
        testUser2.setDisplayName("Test User 2");
        testUser2.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2"); // BCrypt hash for "password"
        testUser2.setStatus(User.UserStatus.PENDING);
        testUser2.setAuthProvider(User.AuthProvider.LOCAL);
        testUser2.setRoles(Set.of(userRole));

        testUser3 = new User();
        testUser3.setUsername("adminuser");
        testUser3.setEmail("admin@example.com");
        testUser3.setDisplayName("Admin User");
        testUser3.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2"); // BCrypt hash for "password"
        testUser3.setStatus(User.UserStatus.ACTIVE);
        testUser3.setAuthProvider(User.AuthProvider.OAUTH2);
        testUser3.setExternalId("oauth2_123");
        testUser3.setRoles(Set.of(adminRole, userRole));

        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
        entityManager.persistAndFlush(testUser3);
    }

    @Test
    void testFindByUsername() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("testuser1");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test1@example.com");
        assertThat(foundUser.get().getStatus()).isEqualTo(User.UserStatus.ACTIVE);
    }

    @Test
    void testFindByUsername_NotFound() {
        // When
        Optional<User> foundUser = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void testFindByEmail() {
        // When
        Optional<User> foundUser = userRepository.findByEmail("admin@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("adminuser");
        assertThat(foundUser.get().getAuthProvider()).isEqualTo(User.AuthProvider.OAUTH2);
    }

    @Test
    void testFindByExternalIdAndAuthProvider() {
        // When
        Optional<User> foundUser = userRepository.findByExternalIdAndAuthProvider(
            "oauth2_123", User.AuthProvider.OAUTH2);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("adminuser");
        assertThat(foundUser.get().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void testFindByStatus() {
        // When
        List<User> activeUsers = userRepository.findByStatus(User.UserStatus.ACTIVE);
        List<User> pendingUsers = userRepository.findByStatus(User.UserStatus.PENDING);

        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting(User::getUsername)
                                .containsExactlyInAnyOrder("testuser1", "adminuser");

        assertThat(pendingUsers).hasSize(1);
        assertThat(pendingUsers.get(0).getUsername()).isEqualTo("testuser2");
    }

    @Test
    void testExistsByUsername() {
        // When & Then
        assertThat(userRepository.existsByUsername("testuser1")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    void testExistsByEmail() {
        // When & Then
        assertThat(userRepository.existsByEmail("test1@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    void testFindByRoleName() {
        // When
        List<User> adminUsers = userRepository.findByRoleName("ADMIN");
        List<User> regularUsers = userRepository.findByRoleName("USER");

        // Then
        assertThat(adminUsers).hasSize(1);
        assertThat(adminUsers.get(0).getUsername()).isEqualTo("adminuser");

        assertThat(regularUsers).hasSize(3); // 所有用户都有USER角色
        assertThat(regularUsers).extracting(User::getUsername)
                                .containsExactlyInAnyOrder("testuser1", "testuser2", "adminuser");
    }

    @Test
    void testFindPendingUsers() {
        // Given - 添加更多待审批用户
        User pendingUser2 = new User();
        pendingUser2.setUsername("pending2");
        pendingUser2.setEmail("pending2@example.com");
        pendingUser2.setDisplayName("Pending User 2");
        pendingUser2.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        pendingUser2.setStatus(User.UserStatus.PENDING);
        pendingUser2.setAuthProvider(User.AuthProvider.LOCAL);
        entityManager.persistAndFlush(pendingUser2);

        // When
        List<User> pendingUsers = userRepository.findPendingUsers();

        // Then
        assertThat(pendingUsers).hasSize(2);
        assertThat(pendingUsers).extracting(User::getUsername)
                                .containsExactlyInAnyOrder("testuser2", "pending2");
        
        // 验证按创建时间排序
        assertThat(pendingUsers.get(0).getCreatedAt())
            .isBeforeOrEqualTo(pendingUsers.get(1).getCreatedAt());
    }

    @Test
    void testCrudOperations() {
        // Create
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setDisplayName("New User");
        newUser.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        newUser.setStatus(User.UserStatus.ACTIVE);
        newUser.setAuthProvider(User.AuthProvider.LOCAL);

        User savedUser = userRepository.save(newUser);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        // Read
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("newuser");

        // Update
        savedUser.setEmail("updated@example.com");
        User updatedUser = userRepository.save(savedUser);
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getUpdatedAt()).isNotNull();

        // Delete
        userRepository.delete(updatedUser);
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    void testFindAll() {
        // When
        List<User> allUsers = userRepository.findAll();

        // Then
        assertThat(allUsers).hasSize(3);
        assertThat(allUsers).extracting(User::getUsername)
                           .containsExactlyInAnyOrder("testuser1", "testuser2", "adminuser");
    }

    @Test
    void testCount() {
        // When
        long count = userRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testDeleteById() {
        // Given
        Long userId = testUser1.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        Optional<User> deletedUser = userRepository.findById(userId);
        assertThat(deletedUser).isEmpty();
        
        long remainingCount = userRepository.count();
        assertThat(remainingCount).isEqualTo(2);
    }
}