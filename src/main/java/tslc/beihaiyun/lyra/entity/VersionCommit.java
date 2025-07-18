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
 * 版本提交实体类
 * 管理Git版本控制的提交记录
 */
@Entity
@Table(name = "version_commits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class VersionCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "提交哈希不能为空")
    @Size(max = 40, message = "提交哈希不能超过40个字符")
    @Column(name = "commit_hash", nullable = false, unique = true)
    private String commitHash;

    @NotBlank(message = "仓库路径不能为空")
    @Size(max = 1000, message = "仓库路径不能超过1000个字符")
    @Column(name = "repository_path", nullable = false)
    private String repositoryPath;

    @NotBlank(message = "提交消息不能为空")
    @Size(max = 1000, message = "提交消息不能超过1000个字符")
    @Column(nullable = false)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "commit_time", nullable = false)
    private LocalDateTime commitTime;

    @Column(name = "parent_commit_hash")
    private String parentCommitHash;

    @Column(name = "tree_hash")
    private String treeHash;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "insertions")
    private Integer insertions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}