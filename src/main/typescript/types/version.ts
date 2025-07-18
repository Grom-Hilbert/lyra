import { BaseEntity } from './common';
import { User } from './user';

/**
 * 版本提交接口
 */
export interface VersionCommit extends BaseEntity {
  commitHash: string;
  repositoryPath: string;
  message: string;
  author: User;
  authorEmail?: string;
  commitTime: string;
  parentCommitHash?: string;
  treeHash?: string;
  filesChanged?: number;
  insertions?: number;
  deletions?: number;
}

/**
 * 版本控制初始化请求
 */
export interface InitVersionControlRequest {
  folderId: number;
  versionControlType: 'BASIC' | 'ADVANCED';
  remoteRepositoryUrl?: string;
  initialCommitMessage?: string;
}

/**
 * 版本提交请求
 */
export interface CommitRequest {
  repositoryPath: string;
  message: string;
  files?: string[];
  authorName?: string;
  authorEmail?: string;
}

/**
 * 版本提交响应
 */
export interface CommitResponse {
  commitHash: string;
  message: string;
  author: string;
  commitTime: string;
  filesChanged: number;
  insertions: number;
  deletions: number;
}

/**
 * 版本历史请求
 */
export interface VersionHistoryRequest {
  repositoryPath?: string;
  filePath?: string;
  folderId?: number;
  page?: number;
  size?: number;
  since?: string;
  until?: string;
  author?: string;
}

/**
 * 版本历史响应
 */
export interface VersionHistoryResponse {
  commits: CommitInfo[];
  totalCommits: number;
  hasMore: boolean;
}

/**
 * 提交信息
 */
export interface CommitInfo {
  hash: string;
  shortHash: string;
  message: string;
  author: string;
  authorEmail: string;
  commitTime: string;
  parentHashes: string[];
  filesChanged: number;
  insertions: number;
  deletions: number;
  tags: string[];
  branches: string[];
}

/**
 * 版本回滚请求
 */
export interface RevertRequest {
  repositoryPath: string;
  commitHash: string;
  createBackup?: boolean;
  revertMessage?: string;
}

/**
 * 版本比较请求
 */
export interface CompareVersionsRequest {
  repositoryPath: string;
  fromCommit: string;
  toCommit: string;
  filePath?: string;
}

/**
 * 版本比较响应
 */
export interface CompareVersionsResponse {
  fromCommit: CommitInfo;
  toCommit: CommitInfo;
  changes: FileChange[];
  summary: {
    filesChanged: number;
    insertions: number;
    deletions: number;
  };
}

/**
 * 文件变更信息
 */
export interface FileChange {
  filePath: string;
  changeType: 'ADDED' | 'MODIFIED' | 'DELETED' | 'RENAMED' | 'COPIED';
  oldPath?: string;
  insertions: number;
  deletions: number;
  isBinary: boolean;
  diff?: string;
}

/**
 * 分支信息
 */
export interface BranchInfo {
  name: string;
  isDefault: boolean;
  isRemote: boolean;
  lastCommit: CommitInfo;
  ahead: number;
  behind: number;
}

/**
 * 标签信息
 */
export interface TagInfo {
  name: string;
  commit: CommitInfo;
  message?: string;
  tagger?: string;
  tagTime?: string;
}

/**
 * Git状态信息
 */
export interface GitStatus {
  repositoryPath: string;
  currentBranch: string;
  isClean: boolean;
  staged: string[];
  unstaged: string[];
  untracked: string[];
  conflicted: string[];
  ahead: number;
  behind: number;
}

/**
 * 远程仓库配置
 */
export interface RemoteRepositoryConfig {
  name: string;
  url: string;
  username?: string;
  password?: string;
  sshKey?: string;
  isDefault: boolean;
}

/**
 * 同步请求
 */
export interface SyncRequest {
  repositoryPath: string;
  remoteName?: string;
  branch?: string;
  force?: boolean;
}

/**
 * 同步响应
 */
export interface SyncResponse {
  success: boolean;
  message: string;
  pulledCommits: number;
  pushedCommits: number;
  conflicts: string[];
}

/**
 * 文件格式转换请求
 */
export interface FileConversionRequest {
  filePath: string;
  sourceFormat: string;
  targetFormat: string;
  options?: Record<string, any>;
}

/**
 * 文件格式转换响应
 */
export interface FileConversionResponse {
  success: boolean;
  convertedFilePath: string;
  originalSize: number;
  convertedSize: number;
  conversionTime: number;
  warnings: string[];
}