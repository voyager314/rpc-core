package com.yzy;

import com.yzy.config.RpcConfig;
import com.yzy.constant.RpcConstant;
import com.yzy.util.ConfigUtil;

/**
 * 简单服务消费者示例
 */
public class ConsumerExample {
    public static void main(String[] args) {
        RpcConfig rpcConfig = ConfigUtil.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        System.out.println(rpcConfig);
    }
}
