package com.yzy.proxy;

import cn.hutool.core.util.IdUtil;
import com.yzy.RpcApplication;
import com.yzy.config.RpcConfig;
import com.yzy.constant.ProtocolConstant;
import com.yzy.constant.RpcConstant;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.protocol.*;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import com.yzy.util.Serializer;
import com.yzy.util.SerializerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
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

        //根据配置文件从序列化器工厂获取
        Serializer serializer = SerializerFactory.getInstance(rpcConfig.getSerializer());
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .serviceName(serviceName)
                .parameterTypes(method.getParameterTypes())
                .args(args).build();//构造请求

        /*HttpResponse httpResponse = HttpRequest.post(selected.getServiceAddr())//发送到远程服务
                .body(serializer.serialize(rpcRequest))
                .execute();
        byte[] result = httpResponse.bodyBytes();
        RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);*/

        //vertx 发送tcp请求
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        Vertx vertx = Vertx.vertx();
        vertx.createNetClient().connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(), result -> {
            if (result.succeeded()) {
                log.info("connect tcp server success");
                NetSocket socket = result.result();
                ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                ProtocolMessage.Header header = protocolMessage.getHeader();
                header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                ProtocolMsgSerializerEnum anEnum = ProtocolMsgSerializerEnum.getEnumByName(rpcConfig.getSerializer());
                if(anEnum==null){
                    throw new RuntimeException("不存在该序列化协议");
                }
                header.setSerializer((byte)anEnum.getCode());
                header.setType((byte)ProtocolMsgTypeEnum.REQUEST.getValue());
                header.setRequestId(IdUtil.getSnowflakeNextId());
                protocolMessage.setHeader(header);
                protocolMessage.setBody(rpcRequest);
                Buffer encode=null;
                try {
                    //编码请求
                    encode=ProtocolMsgEncoder.encode(protocolMessage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //发送
                socket.write(encode);

                //接收响应
                socket.handler(buffer -> {
                    ProtocolMessage<RpcResponse> responseMessage=null;
                    try {
                        responseMessage = (ProtocolMessage<RpcResponse>) ProtocolMsgDecoder.decode(buffer);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    responseFuture.complete(responseMessage.getBody());
                });
            }else log.error("connect tcp server fail");
        });
        RpcResponse rpcResponse = responseFuture.get();
        return rpcResponse.getData();//获取结果
    }
}
