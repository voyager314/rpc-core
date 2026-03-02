package com.yzy.bootstrap;

import com.yzy.RpcApplication;
import com.yzy.annotation.EnableRpc;
import com.yzy.config.RpcConfig;
import com.yzy.server.tcp.VertxTcpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

@Slf4j
public class RpcInitBootStrap implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        boolean needServer= (boolean)metadata.getAnnotationAttributes(EnableRpc.class.getName()).get("needServer");

        //初始化框架并加载配置
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        if(needServer){
            new VertxTcpServer().start(rpcConfig.getServerPort());
        }else log.info("不启动服务器");
    }
}
