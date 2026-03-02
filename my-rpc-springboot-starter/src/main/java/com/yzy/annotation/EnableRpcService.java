package com.yzy.annotation;

import com.yzy.constant.RpcConstant;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务提供者注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)//type表示该注解只能用于类型声明上，如类、接口、注解、枚举
@Component
public @interface EnableRpcService {
    /**
     * 服务接口类
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 版本
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;
}
