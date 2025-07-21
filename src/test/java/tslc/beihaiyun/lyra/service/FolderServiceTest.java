package tslc.beihaiyun.lyra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.entity.User;
import tslc.beihaiyun.lyra.repository.FolderRepository;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件夹管理服务集成测试
 * 测试FolderService的所有核心功能
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文件夹管理服务集成测试")
class FolderServiceTest {

    @Autowired
    private FolderService folderService;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Space testSpace;
    private Folder rootFolder;
    private Folder subFolder;

    @BeforeEach
    void setUp() {
        // 清理数据
        folderRepository.deleteAll();
        spaceRepository.deleteAll();
        userRepository.deleteAll();

        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // 创建测试空间
        testSpace = new Space();
        testSpace.setName("Test Space");
        testSpace.setDescription("Test space for folder operations");
        testSpace.setType(Space.SpaceType.PERSONAL);
        testSpace.setOwner(testUser);
        testSpace.setCreatedBy(testUser.getId().toString());
        testSpace.setUpdatedBy(testUser.getId().toString());
        testSpace = spaceRepository.save(testSpace);

        // 创建测试根文件夹
        FolderService.FolderOperationResult rootResult = folderService.createFolder(
            "Root Folder", null, testSpace, testUser.getId());
        assertTrue(rootResult.isSuccess());
        rootFolder = rootResult.getFolder();

        // 创建测试子文件夹
        FolderService.FolderOperationResult subResult = folderService.createFolder(
            "Sub Folder", rootFolder, testSpace, testUser.getId());
        assertTrue(subResult.isSuccess());
        subFolder = subResult.getFolder();
    }

    // ==================== 基础CRUD操作测试 ====================

    @Test
    @DisplayName("创建文件夹应成功")
    void should_CreateFolder_When_ValidInput() {
        // When
        FolderService.FolderOperationResult result = folderService.createFolder(
            "New Folder", rootFolder, testSpace, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFolder());
        assertEquals("New Folder", result.getFolder().getName());
        assertEquals(testSpace.getId(), result.getFolder().getSpace().getId());
        assertEquals(rootFolder.getId(), result.getFolder().getParent().getId());
        assertEquals(rootFolder.getLevel() + 1, result.getFolder().getLevel());
        assertFalse(result.getFolder().getIsRoot());
    }

    @Test
    @DisplayName("创建根文件夹应成功")
    void should_CreateRootFolder_When_ParentIsNull() {
        // When
        FolderService.FolderOperationResult result = folderService.createFolder(
            "Root Folder 2", null, testSpace, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFolder());
        assertEquals("Root Folder 2", result.getFolder().getName());
        assertNull(result.getFolder().getParent());
        assertEquals(0, result.getFolder().getLevel());
        assertTrue(result.getFolder().getIsRoot());
    }

    @Test
    @DisplayName("创建空名称文件夹应失败")
    void should_FailCreateFolder_When_EmptyName() {
        // When
        FolderService.FolderOperationResult result = folderService.createFolder(
            "", rootFolder, testSpace, testUser.getId());

        // Then
        assertFalse(result.isSuccess());
        assertNull(result.getFolder());
        assertTrue(result.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("创建重复名称文件夹应失败")
    void should_FailCreateFolder_When_DuplicateName() {
        // When
        FolderService.FolderOperationResult result = folderService.createFolder(
            "Sub Folder", rootFolder, testSpace, testUser.getId());

        // Then
        assertFalse(result.isSuccess());
        assertNull(result.getFolder());
        assertTrue(result.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("根据ID获取文件夹应成功")
    void should_GetFolderById_When_FolderExists() {
        // When
        Optional<Folder> result = folderService.getFolderById(rootFolder.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(rootFolder.getId(), result.get().getId());
        assertEquals("Root Folder", result.get().getName());
    }

    @Test
    @DisplayName("根据不存在的ID获取文件夹应返回空")
    void should_ReturnEmpty_When_FolderNotExists() {
        // When
        Optional<Folder> result = folderService.getFolderById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("根据路径获取文件夹应成功")
    void should_GetFolderByPath_When_PathExists() {
        // When
        Optional<Folder> result = folderService.getFolderByPath(testSpace, rootFolder.getPath());

        // Then
        assertTrue(result.isPresent());
        assertEquals(rootFolder.getId(), result.get().getId());
    }

    @Test
    @DisplayName("更新文件夹信息应成功")
    void should_UpdateFolderInfo_When_ValidInput() {
        // When
        FolderService.FolderOperationResult result = folderService.updateFolderInfo(
            rootFolder.getId(), "Updated Root Folder", testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFolder());
        assertEquals("Updated Root Folder", result.getFolder().getName());
    }

    @Test
    @DisplayName("删除空文件夹应成功")
    void should_DeleteFolder_When_FolderIsEmpty() {
        // Given
        FolderService.FolderOperationResult createResult = folderService.createFolder(
            "Empty Folder", rootFolder, testSpace, testUser.getId());
        assertTrue(createResult.isSuccess());
        Long emptyFolderId = createResult.getFolder().getId();

        // When
        boolean result = folderService.deleteFolder(emptyFolderId, testUser.getId(), false);

        // Then
        assertTrue(result);
        assertFalse(folderService.getFolderById(emptyFolderId).isPresent());
    }

    @Test
    @DisplayName("删除非空文件夹应失败")
    void should_FailDeleteFolder_When_FolderNotEmpty() {
        // When
        boolean result = folderService.deleteFolder(rootFolder.getId(), testUser.getId(), false);

        // Then
        assertFalse(result);
        assertTrue(folderService.getFolderById(rootFolder.getId()).isPresent());
    }

    @Test
    @DisplayName("强制删除非空文件夹应成功")
    void should_ForceDeleteFolder_When_FolderNotEmpty() {
        // When
        boolean result = folderService.deleteFolder(rootFolder.getId(), testUser.getId(), true);

        // Then
        assertTrue(result);
        assertFalse(folderService.getFolderById(rootFolder.getId()).isPresent());
    }

    // ==================== 层级管理测试 ====================

    @Test
    @DisplayName("获取子文件夹列表应成功")
    void should_GetChildFolders_When_FolderHasChildren() {
        // When
        List<Folder> children = folderService.getChildFolders(rootFolder);

        // Then
        assertNotNull(children);
        assertEquals(1, children.size());
        assertEquals(subFolder.getId(), children.get(0).getId());
    }

    @Test
    @DisplayName("获取根文件夹列表应成功")
    void should_GetRootFolders_When_SpaceHasRootFolders() {
        // When
        List<Folder> rootFolders = folderService.getRootFolders(testSpace);

        // Then
        assertNotNull(rootFolders);
        assertEquals(1, rootFolders.size());
        assertEquals(rootFolder.getId(), rootFolders.get(0).getId());
    }

    @Test
    @DisplayName("获取祖先文件夹应成功")
    void should_GetAncestorFolders_When_FolderHasAncestors() {
        // When
        List<Folder> ancestors = folderService.getAncestorFolders(subFolder);

        // Then
        assertNotNull(ancestors);
        // 至少应该包含根文件夹
        assertTrue(ancestors.size() >= 1);
    }

    @Test
    @DisplayName("获取后代文件夹应成功")
    void should_GetDescendantFolders_When_FolderHasDescendants() {
        // When
        List<Folder> descendants = folderService.getDescendantFolders(rootFolder);

        // Then
        assertNotNull(descendants);
        assertTrue(descendants.size() >= 1);
    }

    @Test
    @DisplayName("构建文件夹树应成功")
    void should_BuildFolderTree_When_SpaceHasFolders() {
        // When
        List<FolderService.FolderTreeNode> tree = folderService.buildFolderTree(testSpace, -1);

        // Then
        assertNotNull(tree);
        assertEquals(1, tree.size());
        
        FolderService.FolderTreeNode rootNode = tree.get(0);
        assertEquals(rootFolder.getId(), rootNode.getFolder().getId());
        assertEquals(1, rootNode.getChildren().size());
        
        FolderService.FolderTreeNode childNode = rootNode.getChildren().get(0);
        assertEquals(subFolder.getId(), childNode.getFolder().getId());
    }

    @Test
    @DisplayName("移动文件夹应成功")
    void should_MoveFolder_When_ValidTarget() {
        // Given
        FolderService.FolderOperationResult createResult = folderService.createFolder(
            "Target Folder", null, testSpace, testUser.getId());
        assertTrue(createResult.isSuccess());
        Folder targetFolder = createResult.getFolder();

        // When
        FolderService.FolderOperationResult result = folderService.moveFolder(
            subFolder.getId(), targetFolder, testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getFolder());
        assertEquals(targetFolder.getId(), result.getFolder().getParent().getId());
        assertEquals(targetFolder.getLevel() + 1, result.getFolder().getLevel());
    }

    @Test
    @DisplayName("移动文件夹到子文件夹应失败")
    void should_FailMoveFolder_When_TargetIsDescendant() {
        // When
        FolderService.FolderOperationResult result = folderService.moveFolder(
            rootFolder.getId(), subFolder, testUser.getId());

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("子文件夹"));
    }

    @Test
    @DisplayName("重命名文件夹应成功")
    void should_RenameFolder_When_ValidName() {
        // When
        FolderService.FolderOperationResult result = folderService.renameFolder(
            subFolder.getId(), "Renamed Sub Folder", testUser.getId());

        // Then
        assertTrue(result.isSuccess());
        assertEquals("Renamed Sub Folder", result.getFolder().getName());
    }

    // ==================== 批量操作测试 ====================

    @Test
    @DisplayName("批量创建文件夹应成功")
    void should_BatchCreateFolders_When_ValidNames() {
        // Given
        List<String> folderNames = Arrays.asList("Folder1", "Folder2", "Folder3");

        // When
        FolderService.BatchFolderOperationResult result = folderService.batchCreateFolders(
            folderNames, rootFolder, testSpace, testUser.getId());

        // Then
        assertTrue(result.isAllSuccess());
        assertEquals(3, result.getTotalCount());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    @DisplayName("批量创建文件夹部分失败应正确统计")
    void should_BatchCreateFolders_When_SomeNamesDuplicate() {
        // Given
        List<String> folderNames = Arrays.asList("Sub Folder", "New Folder", "Sub Folder");

        // When
        FolderService.BatchFolderOperationResult result = folderService.batchCreateFolders(
            folderNames, rootFolder, testSpace, testUser.getId());

        // Then
        assertFalse(result.isAllSuccess());
        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
    }

    @Test
    @DisplayName("批量删除文件夹应成功")
    void should_BatchDeleteFolders_When_ValidIds() {
        // Given
        FolderService.FolderOperationResult folder1 = folderService.createFolder(
            "Delete1", rootFolder, testSpace, testUser.getId());
        FolderService.FolderOperationResult folder2 = folderService.createFolder(
            "Delete2", rootFolder, testSpace, testUser.getId());
        
        List<Long> folderIds = Arrays.asList(folder1.getFolder().getId(), folder2.getFolder().getId());

        // When
        FolderService.BatchFolderOperationResult result = folderService.batchDeleteFolders(
            folderIds, testUser.getId(), false);

        // Then
        assertTrue(result.isAllSuccess());
        assertEquals(2, result.getSuccessCount());
    }

    @Test
    @DisplayName("批量移动文件夹应成功")
    void should_BatchMoveFolders_When_ValidTarget() {
        // Given
        FolderService.FolderOperationResult folder1 = folderService.createFolder(
            "Move1", rootFolder, testSpace, testUser.getId());
        FolderService.FolderOperationResult folder2 = folderService.createFolder(
            "Move2", rootFolder, testSpace, testUser.getId());
        FolderService.FolderOperationResult targetFolder = folderService.createFolder(
            "Target", null, testSpace, testUser.getId());
        
        List<Long> folderIds = Arrays.asList(folder1.getFolder().getId(), folder2.getFolder().getId());

        // When
        FolderService.BatchFolderOperationResult result = folderService.batchMoveFolders(
            folderIds, targetFolder.getFolder(), testUser.getId());

        // Then
        assertTrue(result.isAllSuccess());
        assertEquals(2, result.getSuccessCount());
    }

    // ==================== 查询和搜索测试 ====================

    @Test
    @DisplayName("分页查询文件夹应正确分页")
    void should_GetFoldersPaged_When_HasFolders() {
        // Given
        folderService.createFolder("Page1", rootFolder, testSpace, testUser.getId());
        folderService.createFolder("Page2", rootFolder, testSpace, testUser.getId());
        
        Pageable pageable = PageRequest.of(0, 2);

        // When
        Page<Folder> result = folderService.getFoldersPaged(testSpace, rootFolder, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements()); // 包括原有的subFolder
        assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("搜索文件夹应返回匹配结果")
    void should_SearchFolders_When_KeywordMatches() {
        // Given
        folderService.createFolder("Search Test", rootFolder, testSpace, testUser.getId());
        folderService.createFolder("Another Folder", rootFolder, testSpace, testUser.getId());

        // When
        List<Folder> result = folderService.searchFolders(testSpace, "Search");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Search Test", result.get(0).getName());
    }

    @Test
    @DisplayName("获取空文件夹列表应成功")
    void should_GetEmptyFolders_When_HasEmptyFolders() {
        // Given
        folderService.createFolder("Empty1", rootFolder, testSpace, testUser.getId());
        folderService.createFolder("Empty2", rootFolder, testSpace, testUser.getId());

        // When
        List<Folder> emptyFolders = folderService.getEmptyFolders(testSpace);

        // Then
        assertNotNull(emptyFolders);
        assertTrue(emptyFolders.size() >= 2);
    }

    @Test
    @DisplayName("获取大文件夹列表应成功")
    void should_GetLargeFolders_When_HasLargeFolders() {
        // When
        List<Folder> largeFolders = folderService.getLargeFolders(testSpace, 0L);

        // Then
        assertNotNull(largeFolders);
        // 应该包含所有文件夹，因为阈值是0
    }

    @Test
    @DisplayName("获取深层文件夹列表应成功")
    void should_GetDeepFolders_When_HasDeepFolders() {
        // When
        List<Folder> deepFolders = folderService.getDeepFolders(testSpace, 0);

        // Then
        assertNotNull(deepFolders);
        assertTrue(deepFolders.size() >= 1); // 至少包含subFolder（level=1）
    }

    // ==================== 统计和信息测试 ====================

    @Test
    @DisplayName("获取文件夹统计应正确")
    void should_GetFolderStatistics_When_HasFolders() {
        // When
        FolderService.FolderStatistics stats = folderService.getFolderStatistics(testSpace);

        // Then
        assertNotNull(stats);
        assertEquals(2, stats.getTotalFolders()); // rootFolder + subFolder
        assertTrue(stats.getMaxDepth() >= 1); // subFolder的层级是1
    }

    @Test
    @DisplayName("计算文件夹大小应正确")
    void should_CalculateFolderSize_When_IncludeSubfolders() {
        // When
        long size = folderService.calculateFolderSize(rootFolder, true);

        // Then
        assertTrue(size >= 0);
    }

    @Test
    @DisplayName("更新文件夹统计信息应成功")
    void should_UpdateFolderStatistics_When_FolderExists() {
        // When
        boolean result = folderService.updateFolderStatistics(rootFolder.getId());

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查文件夹名存在应正确")
    void should_CheckFolderNameExists_When_NameExists() {
        // When
        boolean exists = folderService.isFolderNameExists(rootFolder, "Sub Folder", null);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("检查文件夹名不存在应正确")
    void should_CheckFolderNameNotExists_When_NameNotExists() {
        // When
        boolean exists = folderService.isFolderNameExists(rootFolder, "Non Existent", null);

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("检查文件夹是否为空应正确")
    void should_CheckFolderEmpty_When_FolderIsEmpty() {
        // When
        boolean isEmpty = folderService.isFolderEmpty(subFolder.getId());

        // Then
        assertTrue(isEmpty); // subFolder没有子文件夹和文件
    }

    @Test
    @DisplayName("检查是否可以移动文件夹应正确")
    void should_CheckCanMoveFolder_When_ValidMove() {
        // Given
        FolderService.FolderOperationResult targetResult = folderService.createFolder(
            "Target", null, testSpace, testUser.getId());
        Folder targetFolder = targetResult.getFolder();

        // When
        boolean canMove = folderService.canMoveFolder(subFolder, targetFolder);

        // Then
        assertTrue(canMove);
    }

    @Test
    @DisplayName("检查不能移动到子文件夹应正确")
    void should_CheckCannotMoveFolder_When_TargetIsChild() {
        // When
        boolean canMove = folderService.canMoveFolder(rootFolder, subFolder);

        // Then
        assertFalse(canMove);
    }

    // ==================== 权限和版本控制测试 ====================

    @Test
    @DisplayName("设置版本控制模式应成功")
    void should_SetVersionControlMode_When_FolderExists() {
        // When
        boolean result = folderService.setVersionControlMode(
            rootFolder.getId(), true, false, testUser.getId());

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("继承父文件夹权限应成功")
    void should_InheritParentPermissions_When_FolderExists() {
        // When
        boolean result = folderService.inheritParentPermissions(subFolder.getId(), testUser.getId());

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("应用权限到子文件夹应成功")
    void should_ApplyPermissionsToChildren_When_FolderExists() {
        // When
        boolean result = folderService.applyPermissionsToChildren(rootFolder.getId(), testUser.getId());

        // Then
        assertTrue(result);
    }
} 