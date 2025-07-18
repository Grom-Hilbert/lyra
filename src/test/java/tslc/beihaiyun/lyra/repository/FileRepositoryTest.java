package tslc.beihaiyun.lyra.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import tslc.beihaiyun.lyra.entity.FileEntity;
import tslc.beihaiyun.lyra.entity.FolderEntity;
import tslc.beihaiyun.lyra.entity.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件存储库测试
 * 测试FileRepository的具体功能
 */
@DataJpaTest
@ActiveProfiles("test")
class FileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

    private User testUser;
    private FolderEntity testFolder;
    private FileEntity testFile1;
    private FileEntity testFile2;
    private FileEntity testFile3;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMye.Ik.bDMaByOF.W6OwBnQeQ2TpQdsBq2");
        testUser.setStatus(User.UserStatus.ACTIVE);
        testUser.setAuthProvider(User.AuthProvider.LOCAL);
        entityManager.persistAndFlush(testUser);

        // 创建测试文件夹
        testFolder = new FolderEntity();
        testFolder.setName("testfolder");
        testFolder.setPath("/testfolder");
        testFolder.setOwner(testUser);
        testFolder.setSpaceType(FileEntity.SpaceType.PERSONAL);
        entityManager.persistAndFlush(testFolder);

        // 创建测试文件
        testFile1 = new FileEntity();
        testFile1.setName("test1.txt");
        testFile1.setPath("/testfolder/test1.txt");
        testFile1.setOwner(testUser);
        testFile1.setFolder(testFolder);
        testFile1.setSpaceType(FileEntity.SpaceType.PERSONAL);
        testFile1.setMimeType("text/plain");
        testFile1.setSize(1024L);
        testFile1.setChecksum("checksum1");
        testFile1.setStorageKey("storage_key_1");
        testFile1.setVersionControlType(FileEntity.VersionControlType.BASIC);

        testFile2 = new FileEntity();
        testFile2.setName("test2.pdf");
        testFile2.setPath("/testfolder/test2.pdf");
        testFile2.setOwner(testUser);
        testFile2.setFolder(testFolder);
        testFile2.setSpaceType(FileEntity.SpaceType.ENTERPRISE);
        testFile2.setMimeType("application/pdf");
        testFile2.setSize(2048L);
        testFile2.setChecksum("checksum2");
        testFile2.setStorageKey("storage_key_2");
        testFile2.setVersionControlType(FileEntity.VersionControlType.ADVANCED);

        testFile3 = new FileEntity();
        testFile3.setName("image.jpg");
        testFile3.setPath("/testfolder/image.jpg");
        testFile3.setOwner(testUser);
        testFile3.setFolder(testFolder);
        testFile3.setSpaceType(FileEntity.SpaceType.PERSONAL);
        testFile3.setMimeType("image/jpeg");
        testFile3.setSize(4096L);
        testFile3.setChecksum("checksum3");
        testFile3.setStorageKey("storage_key_3");
        testFile3.setVersionControlType(FileEntity.VersionControlType.NONE);

        entityManager.persistAndFlush(testFile1);
        entityManager.persistAndFlush(testFile2);
        entityManager.persistAndFlush(testFile3);
    }

    @Test
    void testFindByPath() {
        // When
        Optional<FileEntity> foundFile = fileRepository.findByPath("/testfolder/test1.txt");

        // Then
        assertThat(foundFile).isPresent();
        assertThat(foundFile.get().getName()).isEqualTo("test1.txt");
        assertThat(foundFile.get().getMimeType()).isEqualTo("text/plain");
    }

    @Test
    void testFindByPath_NotFound() {
        // When
        Optional<FileEntity> foundFile = fileRepository.findByPath("/nonexistent/file.txt");

        // Then
        assertThat(foundFile).isEmpty();
    }

    @Test
    void testFindByFolder() {
        // When
        List<FileEntity> filesInFolder = fileRepository.findByFolder(testFolder);

        // Then
        assertThat(filesInFolder).hasSize(3);
        assertThat(filesInFolder).extracting(FileEntity::getName)
                                 .containsExactlyInAnyOrder("test1.txt", "test2.pdf", "image.jpg");
    }

    @Test
    void testFindByFolderId() {
        // When
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<FileEntity> filesPage = fileRepository.findByFolderId(testFolder.getId(), pageRequest);

        // Then
        assertThat(filesPage.getContent()).hasSize(3);
        assertThat(filesPage.getTotalElements()).isEqualTo(3);
        assertThat(filesPage.getContent()).extracting(FileEntity::getName)
                                         .containsExactlyInAnyOrder("test1.txt", "test2.pdf", "image.jpg");
    }

    @Test
    void testFindByOwner() {
        // When
        List<FileEntity> userFiles = fileRepository.findByOwner(testUser);

        // Then
        assertThat(userFiles).hasSize(3);
        assertThat(userFiles).allMatch(file -> file.getOwner().equals(testUser));
    }

    @Test
    void testFindBySpaceType() {
        // When
        List<FileEntity> personalFiles = fileRepository.findBySpaceType(FileEntity.SpaceType.PERSONAL);
        List<FileEntity> enterpriseFiles = fileRepository.findBySpaceType(FileEntity.SpaceType.ENTERPRISE);

        // Then
        assertThat(personalFiles).hasSize(2);
        assertThat(personalFiles).extracting(FileEntity::getName)
                                 .containsExactlyInAnyOrder("test1.txt", "image.jpg");

        assertThat(enterpriseFiles).hasSize(1);
        assertThat(enterpriseFiles.get(0).getName()).isEqualTo("test2.pdf");
    }

    @Test
    void testFindByNameContaining() {
        // When
        List<FileEntity> filesWithTest = fileRepository.findByNameContaining("test");
        List<FileEntity> filesWithImage = fileRepository.findByNameContaining("image");

        // Then
        assertThat(filesWithTest).hasSize(2);
        assertThat(filesWithTest).extracting(FileEntity::getName)
                                 .containsExactlyInAnyOrder("test1.txt", "test2.pdf");

        assertThat(filesWithImage).hasSize(1);
        assertThat(filesWithImage.get(0).getName()).isEqualTo("image.jpg");
    }

    @Test
    void testFindByMimeType() {
        // When
        List<FileEntity> textFiles = fileRepository.findByMimeType("text/plain");
        List<FileEntity> pdfFiles = fileRepository.findByMimeType("application/pdf");

        // Then
        assertThat(textFiles).hasSize(1);
        assertThat(textFiles.get(0).getName()).isEqualTo("test1.txt");

        assertThat(pdfFiles).hasSize(1);
        assertThat(pdfFiles.get(0).getName()).isEqualTo("test2.pdf");
    }

    @Test
    void testFindByChecksum() {
        // When
        Optional<FileEntity> foundFile = fileRepository.findByChecksum("checksum1");

        // Then
        assertThat(foundFile).isPresent();
        assertThat(foundFile.get().getName()).isEqualTo("test1.txt");
    }

    @Test
    void testFindByOwnerAndSpaceType() {
        // When
        List<FileEntity> personalFiles = fileRepository.findByOwnerAndSpaceType(
            testUser, FileEntity.SpaceType.PERSONAL);
        List<FileEntity> enterpriseFiles = fileRepository.findByOwnerAndSpaceType(
            testUser, FileEntity.SpaceType.ENTERPRISE);

        // Then
        assertThat(personalFiles).hasSize(2);
        assertThat(personalFiles).extracting(FileEntity::getName)
                                 .containsExactlyInAnyOrder("test1.txt", "image.jpg");

        assertThat(enterpriseFiles).hasSize(1);
        assertThat(enterpriseFiles.get(0).getName()).isEqualTo("test2.pdf");
    }

    @Test
    void testFindByVersionControlType() {
        // When
        List<FileEntity> basicFiles = fileRepository.findByVersionControlType(
            FileEntity.VersionControlType.BASIC);
        List<FileEntity> advancedFiles = fileRepository.findByVersionControlType(
            FileEntity.VersionControlType.ADVANCED);
        List<FileEntity> noneFiles = fileRepository.findByVersionControlType(
            FileEntity.VersionControlType.NONE);

        // Then
        assertThat(basicFiles).hasSize(1);
        assertThat(basicFiles.get(0).getName()).isEqualTo("test1.txt");

        assertThat(advancedFiles).hasSize(1);
        assertThat(advancedFiles.get(0).getName()).isEqualTo("test2.pdf");

        assertThat(noneFiles).hasSize(1);
        assertThat(noneFiles.get(0).getName()).isEqualTo("image.jpg");
    }

    @Test
    void testGetTotalSizeByOwner() {
        // When
        Long totalSize = fileRepository.getTotalSizeByOwner(testUser);

        // Then
        assertThat(totalSize).isEqualTo(7168L); // 1024 + 2048 + 4096
    }

    @Test
    void testCountByFolder() {
        // When
        Long fileCount = fileRepository.countByFolder(testFolder);

        // Then
        assertThat(fileCount).isEqualTo(3L);
    }

    @Test
    void testCrudOperations() {
        // Create
        FileEntity newFile = new FileEntity();
        newFile.setName("newfile.txt");
        newFile.setPath("/testfolder/newfile.txt");
        newFile.setOwner(testUser);
        newFile.setFolder(testFolder);
        newFile.setSpaceType(FileEntity.SpaceType.PERSONAL);
        newFile.setMimeType("text/plain");
        newFile.setSize(512L);
        newFile.setChecksum("newchecksum");
        newFile.setStorageKey("new_storage_key");
        newFile.setVersionControlType(FileEntity.VersionControlType.BASIC);

        FileEntity savedFile = fileRepository.save(newFile);
        assertThat(savedFile.getId()).isNotNull();
        assertThat(savedFile.getCreatedAt()).isNotNull();

        // Read
        Optional<FileEntity> foundFile = fileRepository.findById(savedFile.getId());
        assertThat(foundFile).isPresent();
        assertThat(foundFile.get().getName()).isEqualTo("newfile.txt");

        // Update
        savedFile.setSize(1024L);
        FileEntity updatedFile = fileRepository.save(savedFile);
        assertThat(updatedFile.getSize()).isEqualTo(1024L);
        assertThat(updatedFile.getUpdatedAt()).isNotNull();

        // Delete
        fileRepository.delete(updatedFile);
        Optional<FileEntity> deletedFile = fileRepository.findById(savedFile.getId());
        assertThat(deletedFile).isEmpty();
    }
}