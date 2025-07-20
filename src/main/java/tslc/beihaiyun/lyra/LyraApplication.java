package tslc.beihaiyun.lyra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lyra 企业级云原生文档管理系统主应用类
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@SpringBootApplication
public class LyraApplication {

    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LyraApplication.class, args);
    }
}
