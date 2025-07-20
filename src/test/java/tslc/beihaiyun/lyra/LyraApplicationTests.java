package tslc.beihaiyun.lyra;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Lyra 应用主测试类
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@SpringBootTest
@ActiveProfiles("test")
class LyraApplicationTests {

	/**
	 * 测试Spring应用上下文是否能正常加载
	 */
	@Test
	void contextLoads() {
		// 此测试验证Spring Boot应用能否正常启动
		// 如果上下文加载失败，此测试将失败
	}

	/**
	 * 测试应用配置属性是否正确加载
	 */
	@Test
	void configurationPropertiesLoads() {
		// TODO: 后续会添加配置属性的具体测试
	}
}
