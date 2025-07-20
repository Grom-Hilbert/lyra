package tslc.beihaiyun.lyra.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import tslc.beihaiyun.lyra.config.LyraProperties;
import tslc.beihaiyun.lyra.config.validation.ConfigurationValidator;
import tslc.beihaiyun.lyra.service.ConfigurationManagementService;

import jakarta.validation.Valid;
import java.util.*;

/**
 * 配置管理控制器
 * 提供系统配置的REST API管理接口
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-07-20
 */
@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('ADMIN')")
public class ConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationController.class);

    private final ConfigurationManagementService configService;
    private final ConfigurationValidator configValidator;
    private final LyraProperties lyraProperties;

    @Autowired
    public ConfigurationController(
            ConfigurationManagementService configService,
            ConfigurationValidator configValidator,
            LyraProperties lyraProperties) {
        this.configService = configService;
        this.configValidator = configValidator;
        this.lyraProperties = lyraProperties;
    }

    /**
     * 获取所有配置信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // 获取当前配置
            response.put("current", Map.of(
                "jwt", lyraProperties.getJwt(),
                "storage", lyraProperties.getStorage(),
                "system", lyraProperties.getSystem()
            ));
            
            // 获取动态配置
            response.put("dynamic", configService.getAllDynamicConfigs());
            
            // 获取配置源信息
            response.put("sources", configService.getConfigSourceInfo());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取配置信息失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取配置信息失败：" + e.getMessage()));
        }
    }

    /**
     * 获取特定配置值
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getConfigValue(@PathVariable String key) {
        try {
            String value = configService.getConfigValue(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("key", key);
            response.put("value", value);
            response.put("exists", value != null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取配置值失败: {}", key, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取配置值失败：" + e.getMessage()));
        }
    }

    /**
     * 更新单个配置值
     */
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> updateConfigValue(
            @PathVariable String key,
            @RequestBody Map<String, Object> request) {
        try {
            Object value = request.get("value");
            if (value == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "配置值不能为空"));
            }
            
            boolean success = configService.updateConfigValue(key, value);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("key", key);
            response.put("value", value);
            
            if (success) {
                logger.info("配置已更新: {}={}", key, value);
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "配置更新失败，请检查配置值格式");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("更新配置失败: {}={}", key, request.get("value"), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "更新配置失败：" + e.getMessage()));
        }
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> updateConfigs(
            @RequestBody @Valid Map<String, Object> configs) {
        try {
            Map<String, Boolean> results = configService.updateConfigs(configs);
            
            Map<String, Object> response = new HashMap<>();
            response.put("results", results);
            response.put("totalCount", configs.size());
            response.put("successCount", results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
            response.put("failureCount", results.values().stream().mapToInt(b -> b ? 0 : 1).sum());
            
            logger.info("批量配置更新完成，成功: {}, 失败: {}", 
                response.get("successCount"), response.get("failureCount"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("批量更新配置失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "批量更新配置失败：" + e.getMessage()));
        }
    }

    /**
     * 删除配置值
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, Object>> deleteConfigValue(@PathVariable String key) {
        try {
            boolean success = configService.removeConfigValue(key);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("key", key);
            
            if (success) {
                logger.info("配置已删除: {}", key);
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "配置不存在或删除失败");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("删除配置失败: {}", key, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "删除配置失败：" + e.getMessage()));
        }
    }

    /**
     * 验证配置
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateConfig() {
        try {
            ConfigurationValidator.ValidationResult result = 
                configValidator.validateLyraProperties(lyraProperties);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("hasWarnings", result.hasWarnings());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());
            response.put("infos", result.getInfos());
            response.put("summary", Map.of(
                "errorCount", result.getErrors().size(),
                "warningCount", result.getWarnings().size(),
                "infoCount", result.getInfos().size()
            ));
            
            logger.info("配置验证完成: {}", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("配置验证失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "配置验证失败：" + e.getMessage()));
        }
    }

    /**
     * 刷新配置
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshConfig() {
        try {
            configService.refreshConfiguration();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "配置刷新成功");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("配置已刷新");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("配置刷新失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "配置刷新失败：" + e.getMessage()));
        }
    }

    /**
     * 获取配置默认值
     */
    @GetMapping("/defaults")
    public ResponseEntity<Map<String, Object>> getDefaultConfigs() {
        try {
            // 创建默认配置对象
            LyraProperties defaults = new LyraProperties();
            
            Map<String, Object> response = new HashMap<>();
            response.put("jwt", defaults.getJwt());
            response.put("storage", defaults.getStorage());
            response.put("system", defaults.getSystem());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取默认配置失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "获取默认配置失败：" + e.getMessage()));
        }
    }

    /**
     * 重置配置到默认值
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetToDefaults(
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            List<String> keysToReset = new ArrayList<>();
            
            if (request != null && request.containsKey("keys")) {
                @SuppressWarnings("unchecked")
                List<String> keys = (List<String>) request.get("keys");
                keysToReset.addAll(keys);
            } else {
                // 重置所有动态配置
                keysToReset.addAll(configService.getAllDynamicConfigs().keySet());
            }
            
            int resetCount = 0;
            for (String key : keysToReset) {
                if (configService.removeConfigValue(key)) {
                    resetCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resetCount", resetCount);
            response.put("totalCount", keysToReset.size());
            response.put("message", "配置重置完成");
            
            logger.info("配置已重置，重置数量: {}", resetCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("重置配置失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "重置配置失败：" + e.getMessage()));
        }
    }

    /**
     * 导出配置
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportConfig() {
        try {
            Map<String, Object> exportData = new HashMap<>();
            
            // 当前配置
            exportData.put("current", Map.of(
                "jwt", lyraProperties.getJwt(),
                "storage", lyraProperties.getStorage(),
                "system", lyraProperties.getSystem()
            ));
            
            // 动态配置
            exportData.put("dynamic", configService.getAllDynamicConfigs());
            
            // 元数据
            exportData.put("metadata", Map.of(
                "exportTime", System.currentTimeMillis(),
                "version", "1.0.0",
                "type", "lyra-configuration"
            ));
            
            logger.info("配置已导出");
            
            return ResponseEntity.ok(exportData);
            
        } catch (Exception e) {
            logger.error("导出配置失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "导出配置失败：" + e.getMessage()));
        }
    }

    /**
     * 导入配置
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importConfig(
            @RequestBody Map<String, Object> importData) {
        try {
            if (!importData.containsKey("dynamic")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "导入数据格式无效"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> dynamicConfigs = (Map<String, Object>) importData.get("dynamic");
            
            Map<String, Boolean> results = configService.updateConfigs(dynamicConfigs);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("results", results);
            response.put("importCount", dynamicConfigs.size());
            response.put("successCount", results.values().stream().mapToInt(b -> b ? 1 : 0).sum());
            response.put("message", "配置导入完成");
            
            logger.info("配置已导入，成功: {}", response.get("successCount"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("导入配置失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "导入配置失败：" + e.getMessage()));
        }
    }
} 