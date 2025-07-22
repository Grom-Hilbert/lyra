package tslc.beihaiyun.lyra.controller;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import tslc.beihaiyun.lyra.dto.FolderRequest;
import tslc.beihaiyun.lyra.dto.FolderResponse;
import tslc.beihaiyun.lyra.entity.Folder;
import tslc.beihaiyun.lyra.entity.Space;
import tslc.beihaiyun.lyra.repository.SpaceRepository;
import tslc.beihaiyun.lyra.security.LyraUserPrincipal;
import tslc.beihaiyun.lyra.service.FolderService;

/**
 * FolderController单元测试
 * 使用纯Mockito进行单元测试，避免Spring Boot AOT问题
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FolderControllerUnitTest {

    @Mock
    private FolderService folderService;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private LyraUserPrincipal userPrincipal;

    @InjectMocks
    private FolderController folderController;

    private Space testSpace;
    private Folder testFolder;
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testSpace = new Space();
        testSpace.setId(1L);
        testSpace.setName("测试空间");

        testFolder = new Folder();
        testFolder.setId(1L);
        testFolder.setName("测试文件夹");
        testFolder.setPath("/测试文件夹");
        testFolder.setSpace(testSpace);

        // 模拟用户主体
        when(userPrincipal.getId()).thenReturn(100L);
        when(userPrincipal.getUsername()).thenReturn("testuser");

        // 初始化绑定结果
        bindingResult = new BeanPropertyBindingResult(new Object(), "request");
    }

    @Test
    @DisplayName("创建文件夹 - 成功")
    void should_createFolder_whenValidRequest() {
        // 准备请求
        FolderRequest.CreateFolderRequest request = new FolderRequest.CreateFolderRequest();
        request.setName("新文件夹");
        request.setSpaceId(1L);

        // 模拟服务调用
        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));
        
        Folder createdFolder = new Folder();
        createdFolder.setId(2L);
        createdFolder.setName("测试文件夹");  // 修改为与请求和断言一致的名称
        createdFolder.setPath("/测试文件夹");
        
        FolderService.FolderOperationResult result = 
            new FolderService.FolderOperationResult(true, "创建成功", createdFolder);
        when(folderService.createFolder(any(), any(), any(), anyLong())).thenReturn(result);

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> response = 
                folderController.createFolder(request, userPrincipal);

        // 验证结果
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertEquals("文件夹创建成功", response.getBody().getMessage()); // 修改为与控制器实际消息一致
        assertNotNull(response.getBody().getData());
        assertEquals("测试文件夹", response.getBody().getData().getFolder().getName());
    }

    @Test
    @DisplayName("创建文件夹 - 空间不存在")
    void should_returnBadRequest_whenSpaceNotExists() {
        // 准备请求
        FolderRequest.CreateFolderRequest request = new FolderRequest.CreateFolderRequest();
        request.setName("新文件夹");
        request.setSpaceId(999L);

        // 模拟服务调用
        when(spaceRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> response = 
            folderController.createFolder(request, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("指定的空间不存在", response.getBody().getMessage());
    }

    @Test
    @DisplayName("获取文件夹详情 - 成功")
    void should_getFolderDetail_whenFolderExists() {
        // 模拟服务调用
        when(folderService.getFolderById(1L)).thenReturn(Optional.of(testFolder));

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderDetailResponse>> response = 
            folderController.getFolderDetail(1L, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertNotNull(response.getBody().getData());
        assertEquals(1L, response.getBody().getData().getId());
        assertEquals("测试文件夹", response.getBody().getData().getName());
    }

    @Test
    @DisplayName("获取文件夹详情 - 文件夹不存在")
    void should_returnNotFound_whenFolderNotExists() {
        // 模拟服务调用
        when(folderService.getFolderById(999L)).thenReturn(Optional.empty());

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderDetailResponse>> response = 
            folderController.getFolderDetail(999L, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("文件夹不存在", response.getBody().getMessage());
    }

    @Test
    @DisplayName("更新文件夹 - 成功")
    void should_updateFolder_whenValidRequest() {
        // 准备请求
        FolderRequest.UpdateFolderRequest request = new FolderRequest.UpdateFolderRequest();
        request.setName("更新后的文件夹");

        // 模拟服务调用
        when(folderService.getFolderById(1L)).thenReturn(Optional.of(testFolder));
        
        Folder updatedFolder = new Folder();
        updatedFolder.setId(1L);
        updatedFolder.setName("更新后的文件夹");
        updatedFolder.setPath("/更新后的文件夹");
        
        FolderService.FolderOperationResult result = 
            new FolderService.FolderOperationResult(true, "更新成功", updatedFolder);
        when(folderService.updateFolderInfo(anyLong(), any(), anyLong())).thenReturn(result);

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> response = 
            folderController.updateFolder(1L, request, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertEquals("文件夹更新成功", response.getBody().getMessage());
    }

    @Test
    @DisplayName("删除文件夹 - 成功")
    void should_deleteFolder_whenFolderExists() {
        // 模拟服务调用
        when(folderService.getFolderById(1L)).thenReturn(Optional.of(testFolder));
        when(folderService.deleteFolder(anyLong(), anyLong(), any(Boolean.class))).thenReturn(true);

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<Void>> response = 
            folderController.deleteFolder(1L, true, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getSuccess());
        assertEquals("文件夹删除成功", response.getBody().getMessage());
    }

    @Test
    @DisplayName("删除文件夹 - 删除失败")
    void should_returnBadRequest_whenDeleteFails() {
        // 模拟服务调用
        when(folderService.getFolderById(1L)).thenReturn(Optional.of(testFolder));
        when(folderService.deleteFolder(anyLong(), anyLong(), any(Boolean.class))).thenReturn(false);

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<Void>> response = 
            folderController.deleteFolder(1L, false, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("文件夹删除失败", response.getBody().getMessage());
    }

    @Test
    @DisplayName("验证错误处理 - 异常情况")
    void should_handleException_whenServiceThrowsException() {
        // 准备请求
        FolderRequest.CreateFolderRequest request = new FolderRequest.CreateFolderRequest();
        request.setName("测试文件夹");
        request.setSpaceId(1L);

        // 模拟异常
        when(spaceRepository.findById(1L)).thenThrow(new RuntimeException("数据库连接失败"));

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> response = 
            folderController.createFolder(request, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertTrue(response.getBody().getMessage().contains("服务器内部错误"));
    }

    @Test
    @DisplayName("验证业务逻辑 - 创建操作失败")
    void should_returnBadRequest_whenCreateOperationFails() {
        // 准备请求
        FolderRequest.CreateFolderRequest request = new FolderRequest.CreateFolderRequest();
        request.setName("重复文件夹");
        request.setSpaceId(1L);

        // 模拟服务调用
        when(spaceRepository.findById(1L)).thenReturn(Optional.of(testSpace));
        
        FolderService.FolderOperationResult result = 
            new FolderService.FolderOperationResult(false, "文件夹名称已存在", (Folder) null);
        when(folderService.createFolder(any(), any(), any(), anyLong())).thenReturn(result);

        // 执行测试
        ResponseEntity<FolderResponse.ApiResponse<FolderResponse.FolderOperationResponse>> response = 
            folderController.createFolder(request, userPrincipal);

        // 验证结果
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getSuccess());
        assertEquals("文件夹名称已存在", response.getBody().getMessage());
    }
} 