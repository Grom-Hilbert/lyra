package tslc.beihaiyun.lyra.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * WebDAV 配置测试
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@SpringBootTest
@TestPropertySource(properties = {
    "lyra.webdav.enabled=true",
    "lyra.webdav.base-path=/webdav",
    "lyra.webdav.digest-auth=false"
})
class WebDavConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void webDavConfigBeanExists() {
        // 验证 WebDAV 配置类被正确创建
        assertTrue(applicationContext.containsBean("webDavConfig"));
        
        WebDavConfig config = applicationContext.getBean(WebDavConfig.class);
        assertNotNull(config);
    }

    @Test
    void webDavConfigConditionalProperty() {
        // 验证条件属性生效
        WebDavConfig config = applicationContext.getBean(WebDavConfig.class);
        assertNotNull(config, "WebDAV配置应该在lyra.webdav.enabled=true时被创建");
    }
}

/**
 * WebDAV 禁用时的配置测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "lyra.webdav.enabled=false"
})
class WebDavConfigDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void webDavConfigNotCreatedWhenDisabled() {
        // 当 WebDAV 被禁用时，配置类不应该被创建
        assertFalse(applicationContext.containsBean("webDavConfig"),
            "WebDAV配置在被禁用时不应该被创建");
    }
} 