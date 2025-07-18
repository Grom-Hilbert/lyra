/**
 * TypeScript 数据验证工具
 */

/**
 * 验证结果接口
 */
export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

/**
 * 验证规则接口
 */
export interface ValidationRule<T = any> {
  validate: (value: T) => boolean;
  message: string;
}

/**
 * 字段验证器
 */
export class FieldValidator<T = any> {
  private rules: ValidationRule<T>[] = [];

  /**
   * 添加验证规则
   */
  addRule(rule: ValidationRule<T>): this {
    this.rules.push(rule);
    return this;
  }

  /**
   * 必填验证
   */
  required(message: string = '此字段为必填项'): this {
    return this.addRule({
      validate: (value: T) => value !== null && value !== undefined && value !== '',
      message
    });
  }

  /**
   * 最小长度验证
   */
  minLength(min: number, message?: string): this {
    return this.addRule({
      validate: (value: T) => {
        if (typeof value === 'string') {
          return value.length >= min;
        }
        return true;
      },
      message: message || `最少需要${min}个字符`
    });
  }

  /**
   * 最大长度验证
   */
  maxLength(max: number, message?: string): this {
    return this.addRule({
      validate: (value: T) => {
        if (typeof value === 'string') {
          return value.length <= max;
        }
        return true;
      },
      message: message || `最多允许${max}个字符`
    });
  }

  /**
   * 邮箱格式验证
   */
  email(message: string = '邮箱格式不正确'): this {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return this.addRule({
      validate: (value: T) => {
        if (typeof value === 'string') {
          return emailRegex.test(value);
        }
        return true;
      },
      message
    });
  }

  /**
   * 用户名格式验证
   */
  username(message: string = '用户名只能包含字母、数字、下划线和连字符'): this {
    const usernameRegex = /^[a-zA-Z0-9_-]+$/;
    return this.addRule({
      validate: (value: T) => {
        if (typeof value === 'string') {
          return usernameRegex.test(value);
        }
        return true;
      },
      message
    });
  }

  /**
   * 数值范围验证
   */
  range(min: number, max: number, message?: string): this {
    return this.addRule({
      validate: (value: T) => {
        if (typeof value === 'number') {
          return value >= min && value <= max;
        }
        return true;
      },
      message: message || `数值必须在${min}到${max}之间`
    });
  }

  /**
   * 自定义验证规则
   */
  custom(validator: (value: T) => boolean, message: string): this {
    return this.addRule({
      validate: validator,
      message
    });
  }

  /**
   * 执行验证
   */
  validate(value: T): ValidationResult {
    const errors: string[] = [];

    for (const rule of this.rules) {
      if (!rule.validate(value)) {
        errors.push(rule.message);
      }
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }
}

/**
 * 对象验证器
 */
export class ObjectValidator<T extends Record<string, any>> {
  private fieldValidators: Map<keyof T, FieldValidator> = new Map();

  /**
   * 添加字段验证器
   */
  field<K extends keyof T>(field: K): FieldValidator<T[K]> {
    const validator = new FieldValidator<T[K]>();
    this.fieldValidators.set(field, validator);
    return validator;
  }

  /**
   * 验证对象
   */
  validate(obj: T): ValidationResult & { fieldErrors: Record<string, string[]> } {
    const errors: string[] = [];
    const fieldErrors: Record<string, string[]> = {};

    for (const [field, validator] of this.fieldValidators) {
      const result = validator.validate(obj[field]);
      if (!result.valid) {
        errors.push(...result.errors);
        fieldErrors[field as string] = result.errors;
      }
    }

    return {
      valid: errors.length === 0,
      errors,
      fieldErrors
    };
  }
}

/**
 * 文件路径验证
 */
export function validateFilePath(path: string): ValidationResult {
  const errors: string[] = [];

  if (!path || path.trim() === '') {
    errors.push('文件路径不能为空');
    return { valid: false, errors };
  }

  if (path.length > 1000) {
    errors.push('文件路径长度不能超过1000个字符');
  }

  // 检查非法字符
  const invalidChars = /[<>:"|?*\x00-\x1f]/;
  if (invalidChars.test(path)) {
    errors.push('文件路径包含非法字符');
  }

  // 检查保留名称
  const reservedNames = /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i;
  const parts = path.split(/[/\\]/);
  for (const part of parts) {
    if (part.trim() === '') continue;
    
    if (reservedNames.test(part)) {
      errors.push(`文件路径包含系统保留名称: ${part}`);
    }
    
    if (part.startsWith('.') || part.endsWith('.')) {
      if (part !== '.' && part !== '..') {
        errors.push(`文件名不能以点开头或结尾: ${part}`);
      }
    }
  }

  return {
    valid: errors.length === 0,
    errors
  };
}

/**
 * 文件大小验证
 */
export function validateFileSize(size: number, maxSize?: number): ValidationResult {
  const errors: string[] = [];

  if (size < 0) {
    errors.push('文件大小不能为负数');
  }

  if (maxSize && size > maxSize) {
    errors.push(`文件大小不能超过${formatFileSize(maxSize)}`);
  }

  return {
    valid: errors.length === 0,
    errors
  };
}

/**
 * 密码强度验证
 */
export function validatePasswordStrength(password: string): ValidationResult & { strength: 'weak' | 'medium' | 'strong' } {
  const errors: string[] = [];
  let score = 0;

  if (password.length < 8) {
    errors.push('密码长度至少8个字符');
  } else {
    score += 1;
  }

  if (!/[a-z]/.test(password)) {
    errors.push('密码必须包含小写字母');
  } else {
    score += 1;
  }

  if (!/[A-Z]/.test(password)) {
    errors.push('密码必须包含大写字母');
  } else {
    score += 1;
  }

  if (!/[0-9]/.test(password)) {
    errors.push('密码必须包含数字');
  } else {
    score += 1;
  }

  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
    errors.push('密码必须包含特殊字符');
  } else {
    score += 1;
  }

  let strength: 'weak' | 'medium' | 'strong';
  if (score <= 2) {
    strength = 'weak';
  } else if (score <= 4) {
    strength = 'medium';
  } else {
    strength = 'strong';
  }

  return {
    valid: errors.length === 0,
    errors,
    strength
  };
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = bytes;
  let unitIndex = 0;

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }

  return `${size.toFixed(1)}${units[unitIndex]}`;
}

/**
 * 预定义验证器
 */
export const validators = {
  user: () => new ObjectValidator<{
    username: string;
    email: string;
    displayName: string;
    password?: string;
  }>()
    .field('username')
      .required('用户名不能为空')
      .minLength(3, '用户名至少3个字符')
      .maxLength(50, '用户名最多50个字符')
      .username()
    .field('email')
      .required('邮箱不能为空')
      .email()
    .field('displayName')
      .required('显示名称不能为空')
      .maxLength(100, '显示名称最多100个字符'),

  file: () => new ObjectValidator<{
    name: string;
    path: string;
    size: number;
  }>()
    .field('name')
      .required('文件名不能为空')
      .maxLength(255, '文件名最多255个字符')
    .field('path')
      .required('文件路径不能为空')
      .custom(path => validateFilePath(path).valid, '文件路径格式不正确')
    .field('size')
      .required('文件大小不能为空')
      .custom(size => size >= 0, '文件大小不能为负数'),

  folder: () => new ObjectValidator<{
    name: string;
    path: string;
    description?: string;
  }>()
    .field('name')
      .required('文件夹名称不能为空')
      .maxLength(255, '文件夹名称最多255个字符')
    .field('path')
      .required('文件夹路径不能为空')
      .custom(path => validateFilePath(path).valid, '文件夹路径格式不正确')
    .field('description')
      .maxLength(500, '文件夹描述最多500个字符')
};