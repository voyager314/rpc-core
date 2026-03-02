package com.yzy.proxy;

import com.yzy.RpcApplication;
import com.yzy.config.RpcConfig;
import com.yzy.constant.RpcConstant;
import com.yzy.fault.retry.RetryStrategy;
import com.yzy.fault.retry.RetryStrategyFactory;
import com.yzy.fault.tolerant.TolerantFactory;
import com.yzy.fault.tolerant.TolerantStrategy;
import com.yzy.loadbalancer.LoadBalancer;
import com.yzy.loadbalancer.LoadBalancerFactory;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import com.yzy.server.tcp.VertxTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class ServiceProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //从注册中心获取提供者注册的服务地址
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
        //获取负载均衡器
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        Map<String,Object>params=new HashMap<>();
        //将方法名作为负载均衡参数
        params.put("methodName",method.getName());
        ServiceMetaInfo selected = loadBalancer.select(params, serviceMetaInfos);

        //构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .serviceName(serviceName)
                .parameterTypes(method.getParameterTypes())
                .args(args).build();//构造请求
        RpcResponse rpcResponse = null;

        try {
            //获取重试策略
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
            rpcResponse = retryStrategy.doRetry(() -> {
                //发送tcp请求
                return VertxTcpClient.doRequest(rpcRequest, selected);
            });
        } catch (Exception e) {
            //开启容错
            TolerantStrategy tolerantStrategy = TolerantFactory.getInstance(rpcConfig.getTolerantStrategy());
            rpcResponse=tolerantStrategy.doTolerant(null,e);
        }
        return rpcResponse.getData();//获取结果
    }
}
