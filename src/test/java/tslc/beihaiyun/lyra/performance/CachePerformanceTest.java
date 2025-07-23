package tslc.beihaiyun.lyra.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import tslc.beihaiyun.lyra.config.CacheConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存性能测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
class CachePerformanceTest {

    @Autowired
    private CacheManager cacheManager;

    private Cache testCache;

    @BeforeEach
    void setUp() {
        testCache = cacheManager.getCache(CacheConfig.USER_PERMISSIONS_CACHE);
        assertNotNull(testCache);
        testCache.clear();
    }

    @Test
    void testCacheWritePerformance() {
        int iterations = 10000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            testCache.put("key" + i, "value" + i);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("写入 " + iterations + " 个缓存项耗时: " + duration + "ms");
        System.out.println("平均写入时间: " + (double) duration / iterations + "ms/item");

        // 验证性能要求：平均写入时间应该小于0.1ms
        assertTrue(duration < 1000, "写入性能不符合要求，耗时: " + duration + "ms");
    }

    @Test
    void testCacheReadPerformance() {
        // 先写入测试数据
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            testCache.put("key" + i, "value" + i);
        }

        // 测试读取性能
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            Cache.ValueWrapper wrapper = testCache.get("key" + i);
            assertNotNull(wrapper);
            assertEquals("value" + i, wrapper.get());
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("读取 " + iterations + " 个缓存项耗时: " + duration + "ms");
        System.out.println("平均读取时间: " + (double) duration / iterations + "ms/item");

        // 验证性能要求：平均读取时间应该小于0.05ms
        assertTrue(duration < 500, "读取性能不符合要求，耗时: " + duration + "ms");
    }

    @Test
    void testCacheConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread" + threadId + "_key" + i;
                        String value = "thread" + threadId + "_value" + i;
                        
                        // 写入
                        testCache.put(key, value);
                        
                        // 读取
                        Cache.ValueWrapper wrapper = testCache.get(key);
                        if (wrapper != null && value.equals(wrapper.get())) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "并发测试超时");
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        int totalOperations = threadCount * operationsPerThread;

        System.out.println("并发测试: " + threadCount + " 个线程，每线程 " + operationsPerThread + " 次操作");
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("成功操作数: " + successCount.get() + "/" + totalOperations);
        System.out.println("平均操作时间: " + (double) duration / totalOperations + "ms/operation");

        // 验证并发安全性
        assertEquals(totalOperations, successCount.get(), "并发操作失败");
        
        // 验证性能要求：总耗时应该在合理范围内
        assertTrue(duration < 10000, "并发性能不符合要求，耗时: " + duration + "ms");
    }

    @Test
    void testCacheMemoryUsage() {
        // 获取初始内存使用情况
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // 建议进行垃圾回收
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // 写入大量数据
        int iterations = 50000;
        for (int i = 0; i < iterations; i++) {
            testCache.put("memory_test_key" + i, "memory_test_value_" + i + "_with_some_additional_data_to_increase_size");
        }

        // 获取写入后的内存使用情况
        runtime.gc(); // 建议进行垃圾回收
        long afterWriteMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterWriteMemory - initialMemory;

        System.out.println("写入 " + iterations + " 个缓存项后内存使用: " + memoryUsed / 1024 / 1024 + "MB");
        System.out.println("平均每项内存使用: " + memoryUsed / iterations + " bytes");

        // 清理缓存
        testCache.clear();
        runtime.gc(); // 建议进行垃圾回收
        long afterClearMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("清理缓存后内存使用: " + (afterClearMemory - initialMemory) / 1024 / 1024 + "MB");

        // 验证内存使用合理性（这个测试可能因环境而异，所以设置较宽松的限制）
        assertTrue(memoryUsed < 500 * 1024 * 1024, "内存使用过多: " + memoryUsed / 1024 / 1024 + "MB");
    }

    @Test
    void testCacheEvictionPerformance() {
        // 先写入测试数据
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            testCache.put("evict_key" + i, "evict_value" + i);
        }

        // 测试单个键清理性能
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations / 2; i++) {
            testCache.evict("evict_key" + i);
        }
        long singleEvictTime = System.currentTimeMillis() - startTime;

        // 测试全部清理性能
        startTime = System.currentTimeMillis();
        testCache.clear();
        long clearTime = System.currentTimeMillis() - startTime;

        System.out.println("单个键清理 " + (iterations / 2) + " 项耗时: " + singleEvictTime + "ms");
        System.out.println("全部清理耗时: " + clearTime + "ms");

        // 验证清理性能
        assertTrue(singleEvictTime < 1000, "单个键清理性能不符合要求");
        assertTrue(clearTime < 100, "全部清理性能不符合要求");
    }

    @Test
    void testCacheHitRatio() {
        int totalRequests = 1000;
        int uniqueKeys = 100; // 10:1 的重复率

        // 写入初始数据
        for (int i = 0; i < uniqueKeys; i++) {
            testCache.put("hit_ratio_key" + i, "hit_ratio_value" + i);
        }

        int hits = 0;
        long startTime = System.currentTimeMillis();

        // 模拟随机访问
        for (int i = 0; i < totalRequests; i++) {
            int keyIndex = i % uniqueKeys;
            Cache.ValueWrapper wrapper = testCache.get("hit_ratio_key" + keyIndex);
            if (wrapper != null) {
                hits++;
            }
        }

        long endTime = System.currentTimeMillis();
        double hitRatio = (double) hits / totalRequests;

        System.out.println("缓存命中率测试:");
        System.out.println("总请求数: " + totalRequests);
        System.out.println("命中数: " + hits);
        System.out.println("命中率: " + String.format("%.2f%%", hitRatio * 100));
        System.out.println("耗时: " + (endTime - startTime) + "ms");

        // 验证命中率应该接近100%（因为我们访问的都是存在的键）
        assertTrue(hitRatio > 0.99, "缓存命中率过低: " + String.format("%.2f%%", hitRatio * 100));
    }
}
