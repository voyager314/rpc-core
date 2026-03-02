package com.yzy.bootstrap;

import com.yzy.RpcApplication;
import com.yzy.config.RpcConfig;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.model.ServiceRegisterInfo;
import com.yzy.registry.LocalRegister;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import com.yzy.server.tcp.VertxTcpServer;

import java.util.List;

/**
 * 服务提供者启动项
 */
public class ProviderBootStrap {
    public static void init(List<ServiceRegisterInfo<?>>infos) throws Exception {
        for (ServiceRegisterInfo<?> info : infos) {
            String serviceName = info.getServiceName();
            //注册服务
            LocalRegister.putService(serviceName,info.getImplClass());
            //此时RpcApplication init()已经在getRpcConfig()内调用了，框架初始化完毕
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            //获取注册中心
            Register register = RegisterFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
            serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
            //注册服务元信息
            register.register(serviceMetaInfo);

            //启动http服务器并初始化rpc框架
            //new VertxHttpServer().start(rpcConfig.getServerPort());

            //启动tcp服务器并初始化rpc框架
            new VertxTcpServer().start(rpcConfig.getServerPort());
        }
    }
}
