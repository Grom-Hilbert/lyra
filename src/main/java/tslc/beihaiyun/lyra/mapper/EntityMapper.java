package tslc.beihaiyun.lyra.mapper;

import org.springframework.stereotype.Component;
import tslc.beihaiyun.lyra.dto.*;
import tslc.beihaiyun.lyra.entity.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 实体与DTO转换映射器
 */
@Component
public class EntityMapper {

    /**
     * 用户实体转DTO
     */
    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setDisplayName(user.getDisplayName());
        dto.setStatus(user.getStatus());
        dto.setAuthProvider(user.getAuthProvider());
        dto.setExternalId(user.getExternalId());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());

        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(this::toRoleDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * 用户DTO转实体
     */
    public User toUserEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setDisplayName(dto.getDisplayName());
        user.setStatus(dto.getStatus());
        user.setAuthProvider(dto.getAuthProvider());
        user.setExternalId(dto.getExternalId());
        user.setCreatedAt(dto.getCreatedAt());
        user.setUpdatedAt(dto.getUpdatedAt());
        user.setLastLoginAt(dto.getLastLoginAt());

        return user;
    }

    /**
     * 角色实体转DTO
     */
    public RoleDTO toRoleDTO(Role role) {
        if (role == null) {
            return null;
        }

        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setType(role.getType());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                    .map(this::toPermissionDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * 权限实体转DTO
     */
    public PermissionDTO toPermissionDTO(Permission permission) {
        if (permission == null) {
            return null;
        }

        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDescription(permission.getDescription());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        dto.setCreatedAt(permission.getCreatedAt());
        dto.setUpdatedAt(permission.getUpdatedAt());

        return dto;
    }

    /**
     * 文件实体转DTO
     */
    public FileDTO toFileDTO(FileEntity file) {
        if (file == null) {
            return null;
        }

        FileDTO dto = new FileDTO();
        dto.setId(file.getId());
        dto.setName(file.getName());
        dto.setPath(file.getPath());
        dto.setMimeType(file.getMimeType());
        dto.setSize(file.getSize());
        dto.setChecksum(file.getChecksum());
        dto.setSpaceType(file.getSpaceType());
        dto.setVersionControlType(file.getVersionControlType());
        dto.setFolderId(file.getFolder() != null ? file.getFolder().getId() : null);
        dto.setOwner(toUserDTO(file.getOwner()));
        dto.setCreatedAt(file.getCreatedAt());
        dto.setUpdatedAt(file.getUpdatedAt());
        dto.setAccessedAt(file.getAccessedAt());

        if (file.getVersions() != null) {
            dto.setVersions(file.getVersions().stream()
                    .map(this::toFileVersionDTO)
                    .collect(Collectors.toList()));
        }

        if (file.getPermissions() != null) {
            dto.setPermissions(file.getPermissions().stream()
                    .map(this::toFilePermissionDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * 文件版本实体转DTO
     */
    public FileVersionDTO toFileVersionDTO(FileVersion version) {
        if (version == null) {
            return null;
        }

        FileVersionDTO dto = new FileVersionDTO();
        dto.setId(version.getId());
        dto.setFileId(version.getFile().getId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setVersionDescription(version.getVersionDescription());
        dto.setFilePath(version.getFilePath());
        dto.setSize(version.getSize());
        dto.setChecksum(version.getChecksum());
        dto.setGitCommitHash(version.getGitCommitHash());
        dto.setCreatedBy(toUserDTO(version.getCreatedBy()));
        dto.setIsCurrent(version.getIsCurrent());
        dto.setCreatedAt(version.getCreatedAt());

        return dto;
    }

    /**
     * 文件权限实体转DTO
     */
    public FilePermissionDTO toFilePermissionDTO(FilePermission permission) {
        if (permission == null) {
            return null;
        }

        FilePermissionDTO dto = new FilePermissionDTO();
        dto.setId(permission.getId());
        dto.setFileId(permission.getFile().getId());
        dto.setUser(toUserDTO(permission.getUser()));
        dto.setRole(toRoleDTO(permission.getRole()));
        dto.setPermissionType(permission.getPermissionType());
        dto.setGrantedAt(permission.getGrantedAt());
        dto.setExpiresAt(permission.getExpiresAt());
        dto.setGrantedBy(toUserDTO(permission.getGrantedBy()));

        return dto;
    }

    /**
     * 文件信息转换（简化版）
     */
    public FileDTO.FileInfo toFileInfo(FileEntity file) {
        if (file == null) {
            return null;
        }

        FileDTO.FileInfo info = new FileDTO.FileInfo();
        info.setId(file.getId());
        info.setName(file.getName());
        info.setPath(file.getPath());
        info.setType("FILE");
        info.setMimeType(file.getMimeType());
        info.setSize(file.getSize());
        info.setSpaceType(file.getSpaceType());
        info.setVersionControlType(file.getVersionControlType());
        info.setOwner(file.getOwner().getDisplayName());
        info.setCreatedAt(file.getCreatedAt());
        info.setUpdatedAt(file.getUpdatedAt());
        info.setAccessedAt(file.getAccessedAt());
        info.setIsShared(file.getPermissions() != null && !file.getPermissions().isEmpty());
        info.setHasVersions(file.getVersions() != null && !file.getVersions().isEmpty());

        // 设置权限列表
        if (file.getPermissions() != null) {
            info.setPermissions(file.getPermissions().stream()
                    .map(p -> p.getPermissionType().name())
                    .distinct()
                    .collect(Collectors.toList()));
        }

        return info;
    }

    /**
     * 批量转换用户列表
     */
    public List<UserDTO> toUserDTOList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换文件列表
     */
    public List<FileDTO> toFileDTOList(List<FileEntity> files) {
        if (files == null) {
            return null;
        }
        return files.stream()
                .map(this::toFileDTO)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换文件信息列表
     */
    public List<FileDTO.FileInfo> toFileInfoList(List<FileEntity> files) {
        if (files == null) {
            return null;
        }
        return files.stream()
                .map(this::toFileInfo)
                .collect(Collectors.toList());
    }
}