package tslc.beihaiyun.lyra.entity.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StorageQuota 值对象测试类
 * 测试存储配额的各项业务逻辑
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-20
 */
@DisplayName("StorageQuota 值对象测试")
class StorageQuotaTest {

    @Test
    @DisplayName("创建默认存储配额")
    void testCreateDefault() {
        // When
        StorageQuota quota = StorageQuota.createDefault();

        // Then
        assertNotNull(quota);
        assertEquals(StorageQuota.DEFAULT_QUOTA, quota.getQuota());
        assertEquals(0L, quota.getUsed());
        assertTrue(quota.isValid());
    }

    @Test
    @DisplayName("创建指定配额的存储配额对象 - 正常情况")
    void testCreateWithQuotaInGB_Normal() {
        // Given
        Long quotaInGB = 50L; // 50GB

        // When
        StorageQuota quota = StorageQuota.createWithQuotaInGB(quotaInGB);

        // Then
        assertNotNull(quota);
        assertEquals(50L * StorageQuota.BYTES_PER_GB, quota.getQuota());
        assertEquals(0L, quota.getUsed());
    }

    @Test
    @DisplayName("创建指定配额的存储配额对象 - 配额为0")
    void testCreateWithQuotaInGB_ZeroQuota() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> StorageQuota.createWithQuotaInGB(0L),
            "配额为0时应该抛出异常");
    }

    @Test
    @DisplayName("创建指定配额的存储配额对象 - 配额超出最大限制")
    void testCreateWithQuotaInGB_ExceedsMaxLimit() {
        // Given
        Long quotaInGB = 2000L; // 2TB，超出最大限制

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> StorageQuota.createWithQuotaInGB(quotaInGB),
            "配额超出最大限制时应该抛出异常");
    }

    @Test
    @DisplayName("检查是否有足够存储空间 - 有足够空间")
    void testHasEnoughSpace_Sufficient() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used
        Long additionalSize = 1073741824L; // 1GB

        // When
        boolean result = quota.hasEnoughSpace(additionalSize);

        // Then
        assertTrue(result, "应该有足够的存储空间");
    }

    @Test
    @DisplayName("检查是否有足够存储空间 - 空间不足")
    void testHasEnoughSpace_Insufficient() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 9663676416L); // 10GB total, 9GB used
        Long additionalSize = 2147483648L; // 2GB

        // When
        boolean result = quota.hasEnoughSpace(additionalSize);

        // Then
        assertFalse(result, "应该没有足够的存储空间");
    }

    @Test
    @DisplayName("计算存储使用率 - 正常情况")
    void testGetUsageRatio_Normal() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used

        // When
        double ratio = quota.getUsageRatio();

        // Then
        assertEquals(0.5, ratio, 0.01, "使用率应该是50%");
    }

    @Test
    @DisplayName("计算存储使用率 - 配额为0")
    void testGetUsageRatio_ZeroQuota() {
        // Given
        StorageQuota quota = new StorageQuota(0L, 1000L);

        // When
        double ratio = quota.getUsageRatio();

        // Then
        assertEquals(0.0, ratio, "配额为0时使用率应该是0%");
    }

    @Test
    @DisplayName("计算剩余存储空间")
    void testGetRemainingSpace() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used

        // When
        Long remaining = quota.getRemainingSpace();

        // Then
        assertEquals(5368709120L, remaining, "剩余空间应该是5GB");
    }

    @Test
    @DisplayName("增加已使用存储 - 正常情况")
    void testAddUsage_Normal() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used
        Long additionalSize = 1073741824L; // 1GB

        // When
        StorageQuota newQuota = quota.addUsage(additionalSize);

        // Then
        assertNotNull(newQuota);
        assertEquals(10737418240L, newQuota.getQuota());
        assertEquals(6442450944L, newQuota.getUsed()); // 6GB used
        assertNotSame(quota, newQuota, "应该返回新的对象实例");
    }

    @Test
    @DisplayName("增加已使用存储 - 超出配额限制")
    void testAddUsage_ExceedsQuota() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 9663676416L); // 10GB total, 9GB used
        Long additionalSize = 2147483648L; // 2GB

        // When & Then
        assertThrows(IllegalStateException.class, 
            () -> quota.addUsage(additionalSize),
            "超出配额限制时应该抛出异常");
    }

    @Test
    @DisplayName("减少已使用存储")
    void testReduceUsage() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used
        Long reduceSize = 1073741824L; // 1GB

        // When
        StorageQuota newQuota = quota.reduceUsage(reduceSize);

        // Then
        assertNotNull(newQuota);
        assertEquals(10737418240L, newQuota.getQuota());
        assertEquals(4294967296L, newQuota.getUsed()); // 4GB used
    }

    @Test
    @DisplayName("减少已使用存储 - 减少到负数")
    void testReduceUsage_BelowZero() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 1073741824L); // 10GB total, 1GB used
        Long reduceSize = 2147483648L; // 2GB

        // When
        StorageQuota newQuota = quota.reduceUsage(reduceSize);

        // Then
        assertNotNull(newQuota);
        assertEquals(0L, newQuota.getUsed(), "已使用存储不应该为负数");
    }

    @Test
    @DisplayName("更新配额 - 正常情况")
    void testUpdateQuota_Normal() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used
        Long newQuotaSize = 21474836480L; // 20GB

        // When
        StorageQuota newQuota = quota.updateQuota(newQuotaSize);

        // Then
        assertNotNull(newQuota);
        assertEquals(21474836480L, newQuota.getQuota());
        assertEquals(5368709120L, newQuota.getUsed());
    }

    @Test
    @DisplayName("更新配额 - 新配额小于已使用量")
    void testUpdateQuota_BelowUsedSize() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used
        Long newQuotaSize = 2147483648L; // 2GB

        // When & Then
        assertThrows(IllegalStateException.class, 
            () -> quota.updateQuota(newQuotaSize),
            "新配额小于已使用量时应该抛出异常");
    }

    @Test
    @DisplayName("检查是否接近配额限制")
    void testIsNearQuotaLimit() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 9663676416L); // 10GB total, 9GB used
        double threshold = 0.8; // 80%

        // When
        boolean result = quota.isNearQuotaLimit(threshold);

        // Then
        assertTrue(result, "使用率90%应该超过80%的阈值");
    }

    @Test
    @DisplayName("格式化配额信息")
    void testFormatQuotaInfo() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L); // 10GB total, 5GB used

        // When
        String formatted = quota.formatQuotaInfo();

        // Then
        assertNotNull(formatted);
        assertTrue(formatted.contains("5.0 GB"), "应该包含已使用空间");
        assertTrue(formatted.contains("10.0 GB"), "应该包含总配额");
        assertTrue(formatted.contains("50.0%"), "应该包含使用百分比");
    }

    @Test
    @DisplayName("验证存储配额数据的完整性 - 有效数据")
    void testIsValid_ValidData() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L);

        // When
        boolean result = quota.isValid();

        // Then
        assertTrue(result, "有效的配额数据应该返回true");
    }

    @Test
    @DisplayName("验证存储配额数据的完整性 - 无效数据")
    void testIsValid_InvalidData() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 15000000000L); // used > quota

        // When
        boolean result = quota.isValid();

        // Then
        assertFalse(result, "已使用量超过配额的数据应该返回false");
    }

    @Test
    @DisplayName("测试equals和hashCode")
    void testEqualsAndHashCode() {
        // Given
        StorageQuota quota1 = new StorageQuota(10737418240L, 5368709120L);
        StorageQuota quota2 = new StorageQuota(10737418240L, 5368709120L);
        StorageQuota quota3 = new StorageQuota(21474836480L, 5368709120L);

        // Then
        assertEquals(quota1, quota2, "相同数据的配额对象应该相等");
        assertNotEquals(quota1, quota3, "不同数据的配额对象应该不相等");
        assertEquals(quota1.hashCode(), quota2.hashCode(), "相同对象的hashCode应该相等");
    }

    @Test
    @DisplayName("测试toString")
    void testToString() {
        // Given
        StorageQuota quota = new StorageQuota(10737418240L, 5368709120L);

        // When
        String result = quota.toString();

        // Then
        assertNotNull(result);
        assertTrue(result.contains("StorageQuota"), "toString应该包含类名");
        assertTrue(result.contains("quota="), "toString应该包含配额信息");
        assertTrue(result.contains("used="), "toString应该包含已使用信息");
    }
} 