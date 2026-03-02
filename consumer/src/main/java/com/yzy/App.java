package com.yzy;

import com.yzy.config.RpcConfig;
import com.yzy.constant.RpcConstant;
import com.yzy.util.ConfigUtil;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) {

        RpcConfig rpcConfig = ConfigUtil.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        System.out.println(rpcConfig);
    }
}
