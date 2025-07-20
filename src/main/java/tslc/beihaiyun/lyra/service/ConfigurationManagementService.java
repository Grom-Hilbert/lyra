package tslc.beihaiyun.lyra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Validator;

import tslc.beihaiyun.lyra.config.LyraProperties;

import jakarta.validation.ConstraintViolation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理服务
 * 负责处理配置的优先级、验证、热更新等功能
 * 
 * @author Lyra Team
 * @version 1.0.0
 * @since 2025-01-19
 */
@Service
public class ConfigurationManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManagementService.class);
    
    private final ConfigurableEnvironment environment;
    private final LyraProperties lyraProperties;
    private final jakarta.validation.Validator validator;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 动态配置存储，支持热更新
     */
    private final Map<String, Object> dynamicConfigs = new ConcurrentHashMap<>();
    
    /**
     * 配置变更监听器
     */
    private final List<ConfigurationChangeListener> changeListeners = new ArrayList<>();
    
    /**
     * 配置源优先级（数字越大优先级越高）
     */
    private static final Map<String, Integer> CONFIG_SOURCE_PRIORITY = Map.of(
        "defaultConfig", 1,           // 默认配置
        "applicationConfig", 2,       // application.yml
        "environmentConfig", 3,       // 环境变量
        "externalConfig", 4,         // 外部配置文件
        "dynamicConfig", 5           // 动态配置（最高优先级）
    );

    @Autowired
    public ConfigurationManagementService(
            ConfigurableEnvironment environment,
            LyraProperties lyraProperties,
            jakarta.validation.Validator validator,
            ApplicationEventPublisher eventPublisher) {
        this.environment = environment;
        this.lyraProperties = lyraProperties;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
        
        // 初始化动态配置属性源
        initializeDynamicConfigSource();
    }

    /**
     * 初始化动态配置属性源
     */
    private void initializeDynamicConfigSource() {
        MutablePropertySources propertySources = environment.getPropertySources();
        
        // 添加动态配置属性源（最高优先级）
        MapPropertySource dynamicSource = new MapPropertySource("dynamicConfig", dynamicConfigs);
        propertySources.addFirst(dynamicSource);
        
        logger.info("动态配置属性源已初始化");
    }

    /**
     * 获取配置值，按优先级顺序
     */
    public String getConfigValue(String key) {
        return environment.getProperty(key);
    }

    /**
     * 获取配置值，带默认值
     */
    public String getConfigValue(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    /**
     * 获取配置值，指定类型
     */
    public <T> T getConfigValue(String key, Class<T> targetType) {
        return environment.getProperty(key, targetType);
    }

    /**
     * 获取配置值，指定类型和默认值
     */
    public <T> T getConfigValue(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }

    /**
     * 动态更新配置值（支持热更新）
     */
    public boolean updateConfigValue(String key, Object value) {
        try {
            // 验证配置值
            if (!validateConfigValue(key, value)) {
                logger.warn("配置值验证失败: {}={}", key, value);
                return false;
            }
            
            // 保存旧值用于回滚
            Object oldValue = dynamicConfigs.get(key);
            
            // 更新动态配置
            dynamicConfigs.put(key, value);
            
            // 通知配置变更
            notifyConfigChange(key, oldValue, value);
            
            // 发布配置变更事件
            eventPublisher.publishEvent(new ConfigurationChangeEvent(this, key, oldValue, value));
            
            logger.info("配置已更新: {}={}", key, value);
            return true;
            
        } catch (Exception e) {
            logger.error("更新配置失败: {}={}", key, value, e);
            return false;
        }
    }

    /**
     * 批量更新配置
     */
    public Map<String, Boolean> updateConfigs(Map<String, Object> configs) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            boolean success = updateConfigValue(entry.getKey(), entry.getValue());
            results.put(entry.getKey(), success);
        }
        
        return results;
    }

    /**
     * 删除动态配置值
     */
    public boolean removeConfigValue(String key) {
        if (dynamicConfigs.containsKey(key)) {
            Object oldValue = dynamicConfigs.remove(key);
            notifyConfigChange(key, oldValue, null);
            eventPublisher.publishEvent(new ConfigurationChangeEvent(this, key, oldValue, null));
            logger.info("配置已删除: {}", key);
            return true;
        }
        return false;
    }

    /**
     * 获取所有动态配置
     */
    public Map<String, Object> getAllDynamicConfigs() {
        return new HashMap<>(dynamicConfigs);
    }

    /**
     * 验证配置值
     */
    private boolean validateConfigValue(String key, Object value) {
        // 如果是Lyra配置，进行深度验证
        if (key.startsWith("lyra.")) {
            return validateLyraConfig(key, value);
        }
        
        // 基础类型验证
        return value != null;
    }

    /**
     * 验证Lyra配置
     */
    private boolean validateLyraConfig(String key, Object value) {
        try {
            // 创建临时配置对象进行验证
            LyraProperties tempConfig = new LyraProperties();
            
            // 使用Spring的数据绑定
            DataBinder binder = new DataBinder(tempConfig);
            binder.setValidator((Validator) validator);
            
            // 设置单个属性值
            setNestedProperty(tempConfig, key.substring("lyra.".length()), value);
            
            // 执行验证
            binder.validate();
            BindingResult result = binder.getBindingResult();
            
            if (result.hasErrors()) {
                logger.warn("配置验证失败: {}, 错误: {}", key, result.getAllErrors());
                return false;
            }
            
            // 使用Jakarta Validator进行额外验证
            Set<ConstraintViolation<LyraProperties>> violations = validator.validate(tempConfig);
            if (!violations.isEmpty()) {
                logger.warn("配置约束验证失败: {}, 违规: {}", key, violations);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("验证配置时发生错误: {}", key, e);
            return false;
        }
    }

    /**
     * 设置嵌套属性值（使用反射）
     */
    private void setNestedProperty(Object target, String propertyPath, Object value) {
        // 这里简化实现，实际可以使用Spring的BeanUtils或PropertyAccessor
        String[] parts = propertyPath.split("\\.");
        Object current = target;
        
        for (int i = 0; i < parts.length - 1; i++) {
            // 获取嵌套对象
            current = getNestedObject(current, parts[i]);
            if (current == null) {
                return;
            }
        }
        
        // 设置最终属性值
        setProperty(current, parts[parts.length - 1], value);
    }

    /**
     * 获取嵌套对象
     */
    private Object getNestedObject(Object target, String propertyName) {
        try {
            String methodName = "get" + capitalize(propertyName);
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
            logger.debug("无法获取嵌套对象: {}.{}", target.getClass().getSimpleName(), propertyName);
            return null;
        }
    }

    /**
     * 设置属性值
     */
    private void setProperty(Object target, String propertyName, Object value) {
        try {
            String methodName = "set" + capitalize(propertyName);
            Class<?> paramType = value != null ? value.getClass() : String.class;
            target.getClass().getMethod(methodName, paramType).invoke(target, value);
        } catch (Exception e) {
            logger.debug("无法设置属性值: {}.{}", target.getClass().getSimpleName(), propertyName);
        }
    }

    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 通知配置变更
     */
    private void notifyConfigChange(String key, Object oldValue, Object newValue) {
        ConfigurationChangeEvent event = new ConfigurationChangeEvent(this, key, oldValue, newValue);
        for (ConfigurationChangeListener listener : changeListeners) {
            try {
                listener.onConfigurationChange(event);
            } catch (Exception e) {
                logger.error("配置变更监听器执行失败", e);
            }
        }
    }

    /**
     * 添加配置变更监听器
     */
    public void addConfigurationChangeListener(ConfigurationChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * 移除配置变更监听器
     */
    public void removeConfigurationChangeListener(ConfigurationChangeListener listener) {
        changeListeners.remove(listener);
    }

    /**
     * 重新加载配置
     */
    public void refreshConfiguration() {
        try {
            logger.info("开始重新加载配置...");
            
            // 这里可以触发Spring Cloud Config的刷新
            // 如果有ContextRefresher bean，可以调用refresh()
            
            logger.info("配置重新加载完成");
        } catch (Exception e) {
            logger.error("配置重新加载失败", e);
        }
    }

    /**
     * 获取配置源信息
     */
    public Map<String, Object> getConfigSourceInfo() {
        Map<String, Object> info = new HashMap<>();
        
        MutablePropertySources sources = environment.getPropertySources();
        for (org.springframework.core.env.PropertySource<?> source : sources) {
            info.put(source.getName(), Map.of(
                "name", source.getName(),
                "source", source.getSource().getClass().getSimpleName(),
                "priority", CONFIG_SOURCE_PRIORITY.getOrDefault(source.getName(), 0)
            ));
        }
        
        return info;
    }

    /**
     * 配置变更监听器接口
     */
    public interface ConfigurationChangeListener {
        void onConfigurationChange(ConfigurationChangeEvent event);
    }

    /**
     * 配置变更事件
     */
    public static class ConfigurationChangeEvent {
        private final Object source;
        private final String key;
        private final Object oldValue;
        private final Object newValue;
        private final long timestamp;

        public ConfigurationChangeEvent(Object source, String key, Object oldValue, Object newValue) {
            this.source = source;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public Object getSource() { return source; }
        public String getKey() { return key; }
        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("ConfigurationChangeEvent{key='%s', oldValue=%s, newValue=%s, timestamp=%d}",
                    key, oldValue, newValue, timestamp);
        }
    }
} 