package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件版本实体类
 * 管理文件的版本历史
 */
@Entity
@Table(name = "file_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @NotNull(message = "版本号不能为空")
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Size(max = 500, message = "版本描述不能超过500个字符")
    @Column(name = "version_description")
    private String versionDescription;

    @NotBlank(message = "文件路径不能为空")
    @Size(max = 1000, message = "文件路径不能超过1000个字符")
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @NotNull(message = "文件大小不能为空")
    @Column(nullable = false)
    private Long size;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "git_commit_hash")
    private String gitCommitHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}