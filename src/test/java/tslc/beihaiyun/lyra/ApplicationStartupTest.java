package tslc.beihaiyun.lyra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用启动测试
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationStartupTest {

    /**
     * 测试应用能否在测试环境下正常启动
     */
    @Test
    void applicationStartsSuccessfully() {
        // 如果Spring上下文成功加载，此测试就会通过
        // 这验证了所有必要的beans能正确创建和装配
    }
} 