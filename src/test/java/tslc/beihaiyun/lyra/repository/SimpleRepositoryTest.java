package tslc.beihaiyun.lyra.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 简单Repository测试
 * 用于定位Repository查询问题
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("简单Repository测试")
class SimpleRepositoryTest {

    @Test
    @DisplayName("Repository测试 - 只测试基础功能")
    void testBasicRepositoryFunctionality() {
        // 先测试最基础的功能，不进行任何查询
        // 这里验证DataJpaTest配置是否正确
        assert true;
    }
} 