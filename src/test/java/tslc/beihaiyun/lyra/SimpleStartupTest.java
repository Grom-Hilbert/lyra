package tslc.beihaiyun.lyra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 简单启动测试
 * 验证Spring Boot应用能否正常启动
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("应用启动测试")
class SimpleStartupTest {

    @Test
    @DisplayName("Spring应用上下文启动测试")
    void contextLoads() {
        // 这个测试验证Spring应用上下文能够正常加载
        // 如果有任何配置错误，这个测试会失败
    }
} 