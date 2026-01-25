package com.yzy.config;

import com.yzy.constant.LoadBalancerConstant;
import com.yzy.constant.RetryStrategyKey;
import com.yzy.constant.SerializerKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * rpc框架配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcConfig {
    /**
     * 名称
     */
    private String name = "my-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8081;

    /**
     * 模拟调用
     */
    private boolean mock=false;

    /**
     * 序列化器
     */
    private String serializer= SerializerKey.JDK;

    /**
     * 全局注册中心配置
     */
    private RegistryConfig registryConfig=new RegistryConfig();

    /**
     * 全局负载均衡器
     */
    private String loadBalancer= LoadBalancerConstant.ROUND_ROBIN;

    /**
     * 全局重试策略配置
     */
    private String retryStrategy= RetryStrategyKey.FIXED_INTERVAL;
}
