package com.yzy.bootstrap;

import com.yzy.RpcApplication;
import com.yzy.annotation.EnableRpcService;
import com.yzy.config.RpcConfig;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.registry.LocalRegister;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 服务提供者驱动
 */
public class RpcProviderBootStrap implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        EnableRpcService service = beanClass.getAnnotation(EnableRpcService.class);
        if (service != null) {
            Class<?> interfaceClass = service.interfaceClass();
            if(interfaceClass==void.class){
                //若为空则设为默认值
                interfaceClass = beanClass.getInterfaces()[0];
            }
            String serviceName = interfaceClass.getName();
            LocalRegister.putService(serviceName,beanClass);

            //此时RpcApplication init()已经在getRpcConfig()内调用了，框架初始化完毕
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            //获取注册中心
            Register register = RegisterFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
            serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
            serviceMetaInfo.setServiceVersion(rpcConfig.getVersion());
            //注册服务元信息
            try {
                register.register(serviceMetaInfo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //启动http服务器并初始化rpc框架
            //new VertxHttpServer().start(rpcConfig.getServerPort());


        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
