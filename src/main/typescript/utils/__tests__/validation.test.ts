/**
 * TypeScript 验证工具测试
 */

import {
  FieldValidator,
  ObjectValidator,
  validateFilePath,
  validateFileSize,
  validatePasswordStrength,
  validators
} from '../validation';

describe('FieldValidator', () => {
  test('required validation should work', () => {
    const validator = new FieldValidator<string>().required();
    
    expect(validator.validate('test').valid).toBe(true);
    expect(validator.validate('').valid).toBe(false);
    expect(validator.validate(null as any).valid).toBe(false);
    expect(validator.validate(undefined as any).valid).toBe(false);
  });

  test('minLength validation should work', () => {
    const validator = new FieldValidator<string>().minLength(3);
    
    expect(validator.validate('test').valid).toBe(true);
    expect(validator.validate('ab').valid).toBe(false);
  });

  test('maxLength validation should work', () => {
    const validator = new FieldValidator<string>().maxLength(5);
    
    expect(validator.validate('test').valid).toBe(true);
    expect(validator.validate('toolong').valid).toBe(false);
  });

  test('email validation should work', () => {
    const validator = new FieldValidator<string>().email();
    
    expect(validator.validate('test@example.com').valid).toBe(true);
    expect(validator.validate('invalid-email').valid).toBe(false);
  });

  test('username validation should work', () => {
    const validator = new FieldValidator<string>().username();
    
    expect(validator.validate('test_user-123').valid).toBe(true);
    expect(validator.validate('test@user').valid).toBe(false);
  });

  test('range validation should work', () => {
    const validator = new FieldValidator<number>().range(1, 10);
    
    expect(validator.validate(5).valid).toBe(true);
    expect(validator.validate(0).valid).toBe(false);
    expect(validator.validate(11).valid).toBe(false);
  });

  test('custom validation should work', () => {
    const validator = new FieldValidator<string>()
      .custom(value => value.includes('test'), 'Must contain "test"');
    
    expect(validator.validate('testing').valid).toBe(true);
    expect(validator.validate('example').valid).toBe(false);
  });

  test('chained validations should work', () => {
    const validator = new FieldValidator<string>()
      .required()
      .minLength(3)
      .maxLength(10)
      .username();
    
    expect(validator.validate('test123').valid).toBe(true);
    expect(validator.validate('').valid).toBe(false);
    expect(validator.validate('ab').valid).toBe(false);
    expect(validator.validate('toolongusername').valid).toBe(false);
    expect(validator.validate('test@123').valid).toBe(false);
  });
});

describe('ObjectValidator', () => {
  test('should validate object fields', () => {
    const validator = new ObjectValidator<{
      username: string;
      email: string;
      age: number;
    }>()
      .field('username').required().minLength(3).username()
      .field('email').required().email()
      .field('age').required().range(18, 100);

    const validObject = {
      username: 'testuser',
      email: 'test@example.com',
      age: 25
    };

    const invalidObject = {
      username: 'ab',
      email: 'invalid-email',
      age: 15
    };

    const validResult = validator.validate(validObject);
    expect(validResult.valid).toBe(true);
    expect(Object.keys(validResult.fieldErrors)).toHaveLength(0);

    const invalidResult = validator.validate(invalidObject);
    expect(invalidResult.valid).toBe(false);
    expect(Object.keys(invalidResult.fieldErrors)).toHaveLength(3);
    expect(invalidResult.fieldErrors.username).toContain('用户名至少需要3个字符');
    expect(invalidResult.fieldErrors.email).toContain('邮箱格式不正确');
    expect(invalidResult.fieldErrors.age).toContain('数值必须在18到100之间');
  });
});

describe('validateFilePath', () => {
  test('should validate valid file paths', () => {
    expect(validateFilePath('/valid/path/file.txt').valid).toBe(true);
    expect(validateFilePath('relative/path/file.txt').valid).toBe(true);
    expect(validateFilePath('./current/file.txt').valid).toBe(true);
    expect(validateFilePath('../parent/file.txt').valid).toBe(true);
  });

  test('should reject empty paths', () => {
    expect(validateFilePath('').valid).toBe(false);
    expect(validateFilePath('   ').valid).toBe(false);
  });

  test('should reject paths with invalid characters', () => {
    expect(validateFilePath('/path/with<invalid>chars').valid).toBe(false);
    expect(validateFilePath('/path/with|pipe').valid).toBe(false);
    expect(validateFilePath('/path/with"quote').valid).toBe(false);
  });

  test('should reject reserved names', () => {
    expect(validateFilePath('/path/CON/file.txt').valid).toBe(false);
    expect(validateFilePath('/path/PRN.txt').valid).toBe(false);
    expect(validateFilePath('/path/COM1').valid).toBe(false);
  });

  test('should reject names starting or ending with dots', () => {
    expect(validateFilePath('/path/.hidden').valid).toBe(false);
    expect(validateFilePath('/path/file.').valid).toBe(false);
  });

  test('should reject too long paths', () => {
    const longPath = '/path/' + 'a'.repeat(1000);
    expect(validateFilePath(longPath).valid).toBe(false);
  });
});

describe('validateFileSize', () => {
  test('should validate positive file sizes', () => {
    expect(validateFileSize(0).valid).toBe(true);
    expect(validateFileSize(1024).valid).toBe(true);
    expect(validateFileSize(1024 * 1024).valid).toBe(true);
  });

  test('should reject negative file sizes', () => {
    expect(validateFileSize(-1).valid).toBe(false);
    expect(validateFileSize(-1024).valid).toBe(false);
  });

  test('should respect max size limit', () => {
    const maxSize = 1024 * 1024; // 1MB
    expect(validateFileSize(512 * 1024, maxSize).valid).toBe(true);
    expect(validateFileSize(2 * 1024 * 1024, maxSize).valid).toBe(false);
  });
});

describe('validatePasswordStrength', () => {
  test('should validate strong passwords', () => {
    const result = validatePasswordStrength('StrongP@ssw0rd');
    expect(result.valid).toBe(true);
    expect(result.strength).toBe('strong');
  });

  test('should identify weak passwords', () => {
    const result = validatePasswordStrength('weak');
    expect(result.valid).toBe(false);
    expect(result.strength).toBe('weak');
    expect(result.errors).toContain('密码长度至少8个字符');
  });

  test('should identify medium strength passwords', () => {
    const result = validatePasswordStrength('Medium123');
    expect(result.valid).toBe(false);
    expect(result.strength).toBe('medium');
    expect(result.errors).toContain('密码必须包含特殊字符');
  });

  test('should check all password requirements', () => {
    const result = validatePasswordStrength('password');
    expect(result.valid).toBe(false);
    expect(result.errors).toContain('密码必须包含大写字母');
    expect(result.errors).toContain('密码必须包含数字');
    expect(result.errors).toContain('密码必须包含特殊字符');
  });
});

describe('Predefined validators', () => {
  test('user validator should work', () => {
    const validator = validators.user();
    
    const validUser = {
      username: 'testuser',
      email: 'test@example.com',
      displayName: 'Test User'
    };

    const invalidUser = {
      username: 'ab',
      email: 'invalid-email',
      displayName: ''
    };

    expect(validator.validate(validUser).valid).toBe(true);
    expect(validator.validate(invalidUser).valid).toBe(false);
  });

  test('file validator should work', () => {
    const validator = validators.file();
    
    const validFile = {
      name: 'test.txt',
      path: '/valid/path/test.txt',
      size: 1024
    };

    const invalidFile = {
      name: '',
      path: '/invalid<path>/test.txt',
      size: -1
    };

    expect(validator.validate(validFile).valid).toBe(true);
    expect(validator.validate(invalidFile).valid).toBe(false);
  });

  test('folder validator should work', () => {
    const validator = validators.folder();
    
    const validFolder = {
      name: 'test-folder',
      path: '/valid/path/test-folder'
    };

    const invalidFolder = {
      name: '',
      path: '/invalid<path>/folder',
      description: 'a'.repeat(501) // 超过500字符
    };

    expect(validator.validate(validFolder).valid).toBe(true);
    expect(validator.validate(invalidFolder).valid).toBe(false);
  });
});