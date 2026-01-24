package com.yzy;

import com.yzy.ServiceImpl.UserService;
import com.yzy.config.RpcConfig;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.registry.LocalRegister;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import com.yzy.server.tcp.VertxTcpServer;
import com.yzy.service.IUserService;

/**
 * 简单提供者示例
 */
public class ProviderExample {
    public static void main(String[] args) throws Exception {
        //RpcApplication.setCustomConfig(null);自定义配置，可选

        //注册服务
        String serviceName = IUserService.class.getName();
        LocalRegister.putService(serviceName, UserService.class);
        //此时RpcApplication init()已经在getRpcConfig()内调用了，框架初始化完毕
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        Register register = RegisterFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        register.register(serviceMetaInfo);
        //启动http服务器并初始化rpc框架
        //new VertxHttpServer().start(rpcConfig.getServerPort());

        //启动tcp服务器并初始化rpc框架
        new VertxTcpServer().start(rpcConfig.getServerPort());
    }
}
