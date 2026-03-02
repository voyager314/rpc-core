package com.yzy.annotation;

import com.yzy.bootstrap.RpcConsumerBootStrap;
import com.yzy.bootstrap.RpcInitBootStrap;
import com.yzy.bootstrap.RpcProviderBootStrap;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否启用rpc
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)//type表示该注解只能用于类型声明上，如类、接口、注解、枚举
@Import({RpcConsumerBootStrap.class, RpcProviderBootStrap.class, RpcInitBootStrap.class})
public @interface EnableRpc {
    /**
     * 是否启用服务器
     * @return
     */
    boolean needServer() default true;
}
