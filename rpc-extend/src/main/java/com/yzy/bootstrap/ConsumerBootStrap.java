package com.yzy.bootstrap;

import com.yzy.RpcApplication;

/**
 * 消费者启动项
 */
public class ConsumerBootStrap {
    public static void init(){
        //消费者无需加载服务器，也无需注册服务，直接初始化框架即可
        RpcApplication.getRpcConfig();
    }
}
