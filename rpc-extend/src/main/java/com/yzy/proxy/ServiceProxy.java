package com.yzy.proxy;

import com.yzy.RpcApplication;
import com.yzy.config.RpcConfig;
import com.yzy.constant.RpcConstant;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import com.yzy.server.tcp.VertxTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;


@Slf4j
public class ServiceProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        Register register = RegisterFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        String serviceName = method.getDeclaringClass().getName();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        List<ServiceMetaInfo> serviceMetaInfos = register.serviceDiscovery(serviceMetaInfo.getServiceKey());
        if(serviceMetaInfos==null|| serviceMetaInfos.isEmpty()){
            throw new RuntimeException("暂无服务地址");
        }
        ServiceMetaInfo selected = serviceMetaInfos.get(0);//todo 暂时先选一个

        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .serviceName(serviceName)
                .parameterTypes(method.getParameterTypes())
                .args(args).build();//构造请求

        //发送tcp请求
        RpcResponse rpcResponse = VertxTcpClient.doRequest(rpcRequest, selected);
        return rpcResponse.getData();//获取结果
    }
}
