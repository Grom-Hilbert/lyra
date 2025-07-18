package tslc.beihaiyun.lyra.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 模板文件实体类
 * 管理模板中的文件定义
 */
@Entity
@Table(name = "template_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TemplateFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过255个字符")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "相对路径不能为空")
    @Size(max = 1000, message = "相对路径不能超过1000个字符")
    @Column(name = "relative_path", nullable = false)
    private String relativePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 文件内容（对于文本文件）

    @Column(name = "source_path")
    private String sourcePath; // 源文件路径（对于二进制文件）

    @Column(name = "permissions_config", columnDefinition = "TEXT")
    private String permissionsConfig; // JSON格式的权限配置

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 文件类型枚举
     */
    public enum FileType {
        TEXT,           // 文本文件
        BINARY,         // 二进制文件
        FOLDER          // 文件夹
    }
}