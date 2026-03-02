package com.yzy.annotation;

import com.yzy.constant.LoadBalancerConstant;
import com.yzy.constant.RetryStrategyKey;
import com.yzy.constant.RpcConstant;
import com.yzy.constant.TolerantKey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务消费者使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnableRefrence {
    /**
     * 服务接口类
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 版本
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;

    /**
     * 负载均衡器
     * @return
     */
    String loadBalancer() default LoadBalancerConstant.ROUND_ROBIN;

    /**
     * 容错机制
     * @return
     */
    String tolerantStrategy() default TolerantKey.FAIL_FAST;

    /**
     * 重试机制
     * @return
     */
    String retryStrategy() default RetryStrategyKey.NO;

    /**
     * 是否模拟调用
     * @return
     */
    boolean mock() default false;
}
