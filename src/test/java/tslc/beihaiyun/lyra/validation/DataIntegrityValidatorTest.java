package tslc.beihaiyun.lyra.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import tslc.beihaiyun.lyra.entity.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据完整性验证器测试
 */
@ExtendWith(MockitoExtension.class)
class DataIntegrityValidatorTest {

    @InjectMocks
    private DataIntegrityValidator validator;

    private User testUser;
    private FileEntity testFile;
    private FolderEntity testFolder;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setAuthProvider(User.AuthProvider.LOCAL);
        testUser.setPasswordHash("hashedpassword");

        // 创建测试文件
        testFile = new FileEntity();
        testFile.setId(1L);
        testFile.setName("test.txt");
        testFile.setPath("/test/test.txt");
        testFile.setSize(1024L);
        testFile.setOwner(testUser);
        testFile.setSpaceType(FileEntity.SpaceType.PERSONAL);
        testFile.setVersionControlType(FileEntity.VersionControlType.NONE);

        // 创建测试文件夹
        testFolder = new FolderEntity();
        testFolder.setId(1L);
        testFolder.setName("test");
        testFolder.setPath("/test");
        testFolder.setOwner(testUser);
        testFolder.setSpaceType(FileEntity.SpaceType.PERSONAL);
    }

    @Test
    void testValidateUser_ValidUser_ShouldPass() {
        DataIntegrityValidator.ValidationResult result = validator.validateUser(testUser, true);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateUser_EmptyUsername_ShouldFail() {
        testUser.setUsername("");
        
        DataIntegrityValidator.ValidationResult result = validator.validateUser(testUser, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("用户名不能为空"));
    }

    @Test
    void testValidateUser_InvalidEmail_ShouldFail() {
        testUser.setEmail("invalid-email");
        
        DataIntegrityValidator.ValidationResult result = validator.validateUser(testUser, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("邮箱格式不正确"));
    }

    @Test
    void testValidateUser_InvalidUsername_ShouldFail() {
        testUser.setUsername("ab"); // 太短
        
        DataIntegrityValidator.ValidationResult result = validator.validateUser(testUser, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("用户名格式不正确，只能包含字母、数字、下划线和连字符，长度3-50个字符"));
    }

    @Test
    void testValidateUser_LocalAuthWithoutPassword_ShouldFail() {
        testUser.setPasswordHash(null);
        testUser.setAuthProvider(User.AuthProvider.LOCAL);
        
        DataIntegrityValidator.ValidationResult result = validator.validateUser(testUser, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("本地认证用户必须设置密码"));
    }

    @Test
    void testValidateUser_ExternalAuthWithoutExternalId_ShouldFail() {
        testUser.setAuthProvider(User.AuthProvider.OAUTH2);
        testUser.setExternalId(null);
        
        DataIntegrityValidator.ValidationResult result = validator.validateUser(testUser, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("外部认证用户必须提供外部ID"));
    }

    @Test
    void testValidateFile_ValidFile_ShouldPass() {
        DataIntegrityValidator.ValidationResult result = validator.validateFile(testFile, true);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateFile_EmptyName_ShouldFail() {
        testFile.setName("");
        
        DataIntegrityValidator.ValidationResult result = validator.validateFile(testFile, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("文件名不能为空"));
    }

    @Test
    void testValidateFile_NegativeSize_ShouldFail() {
        testFile.setSize(-1L);
        
        DataIntegrityValidator.ValidationResult result = validator.validateFile(testFile, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("文件大小必须为非负数"));
    }

    @Test
    void testValidateFile_NoOwner_ShouldFail() {
        testFile.setOwner(null);
        
        DataIntegrityValidator.ValidationResult result = validator.validateFile(testFile, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("文件必须有所有者"));
    }

    @Test
    void testValidateFolder_ValidFolder_ShouldPass() {
        DataIntegrityValidator.ValidationResult result = validator.validateFolder(testFolder, true);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateFolder_EmptyName_ShouldFail() {
        testFolder.setName("");
        
        DataIntegrityValidator.ValidationResult result = validator.validateFolder(testFolder, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("文件夹名称不能为空"));
    }

    @Test
    void testValidateFolder_LongDescription_ShouldFail() {
        testFolder.setDescription("a".repeat(501)); // 超过500字符
        
        DataIntegrityValidator.ValidationResult result = validator.validateFolder(testFolder, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("文件夹描述不能超过500个字符"));
    }

    @Test
    void testValidatePermission_ValidPermission_ShouldPass() {
        FilePermission permission = new FilePermission();
        permission.setFile(testFile);
        permission.setUser(testUser);
        permission.setPermissionType(FilePermission.PermissionType.READ);
        permission.setGrantedBy(testUser);
        permission.setGrantedAt(LocalDateTime.now());
        
        DataIntegrityValidator.ValidationResult result = validator.validatePermission(permission);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidatePermission_NoFile_ShouldFail() {
        FilePermission permission = new FilePermission();
        permission.setUser(testUser);
        permission.setPermissionType(FilePermission.PermissionType.READ);
        permission.setGrantedBy(testUser);
        
        DataIntegrityValidator.ValidationResult result = validator.validatePermission(permission);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("权限必须关联文件"));
    }

    @Test
    void testValidatePermission_NoUserOrRole_ShouldFail() {
        FilePermission permission = new FilePermission();
        permission.setFile(testFile);
        permission.setPermissionType(FilePermission.PermissionType.READ);
        permission.setGrantedBy(testUser);
        
        DataIntegrityValidator.ValidationResult result = validator.validatePermission(permission);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("权限必须关联用户或角色"));
    }

    @Test
    void testValidatePermission_ExpiredPermission_ShouldFail() {
        FilePermission permission = new FilePermission();
        permission.setFile(testFile);
        permission.setUser(testUser);
        permission.setPermissionType(FilePermission.PermissionType.READ);
        permission.setGrantedBy(testUser);
        permission.setExpiresAt(LocalDateTime.now().minusDays(1)); // 已过期
        
        DataIntegrityValidator.ValidationResult result = validator.validatePermission(permission);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("权限过期时间不能早于当前时间"));
    }

    @Test
    void testValidateTemplate_ValidTemplate_ShouldPass() {
        Template template = new Template();
        template.setName("Test Template");
        template.setDescription("Test Description");
        template.setTemplateType(Template.TemplateType.FOLDER);
        template.setCreatedBy(testUser);
        
        DataIntegrityValidator.ValidationResult result = validator.validateTemplate(template, true);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateTemplate_EmptyName_ShouldFail() {
        Template template = new Template();
        template.setName("");
        template.setTemplateType(Template.TemplateType.FOLDER);
        template.setCreatedBy(testUser);
        
        DataIntegrityValidator.ValidationResult result = validator.validateTemplate(template, true);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("模板名称不能为空"));
    }

    @Test
    void testValidateVersionCommit_ValidCommit_ShouldPass() {
        VersionCommit commit = new VersionCommit();
        commit.setCommitHash("1234567890123456789012345678901234567890");
        commit.setRepositoryPath("/test/repo");
        commit.setMessage("Test commit");
        commit.setAuthor(testUser);
        commit.setCommitTime(LocalDateTime.now());
        
        DataIntegrityValidator.ValidationResult result = validator.validateVersionCommit(commit);
        
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testValidateVersionCommit_InvalidHashLength_ShouldFail() {
        VersionCommit commit = new VersionCommit();
        commit.setCommitHash("123456"); // 长度不是40
        commit.setRepositoryPath("/test/repo");
        commit.setMessage("Test commit");
        commit.setAuthor(testUser);
        commit.setCommitTime(LocalDateTime.now());
        
        DataIntegrityValidator.ValidationResult result = validator.validateVersionCommit(commit);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("提交哈希长度必须为40个字符"));
    }
}