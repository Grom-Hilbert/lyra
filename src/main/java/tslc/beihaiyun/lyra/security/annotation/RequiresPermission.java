package tslc.beihaiyun.lyra.security.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 * 用于标记需要特定权限才能访问的方法
 * 
 * @author SkyFrost
 * @version 1.0.0
 * @since 2025-01-20
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 所需权限代码
     * 
     * @return 权限代码数组
     */
    String[] value();

    /**
     * 权限逻辑关系
     * AND: 需要同时拥有所有权限
     * OR: 拥有任意一个权限即可
     * 
     * @return 逻辑关系
     */
    Logical logical() default Logical.AND;

    /**
     * 是否检查资源权限
     * 如果为true，将从方法参数中提取资源信息进行权限检查
     * 
     * @return 是否检查资源权限
     */
    boolean checkResource() default false;

    /**
     * 资源类型
     * 当checkResource为true时必须指定
     * 
     * @return 资源类型
     */
    String resourceType() default "";

    /**
     * 错误消息
     * 当权限检查失败时返回的错误消息
     * 
     * @return 错误消息
     */
    String message() default "权限不足，无法访问该资源";

    /**
     * 逻辑关系枚举
     */
    enum Logical {
        /**
         * 逻辑与：需要同时拥有所有权限
         */
        AND,
        
        /**
         * 逻辑或：拥有任意一个权限即可
         */
        OR
    }
} 