package com.yzy.server;

import com.yzy.RpcApplication;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.registry.LocalRegister;
import com.yzy.util.Serializer;
import com.yzy.util.SerializerFactory;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 请求处理器
 */
public class HttpServerHandler implements Handler<HttpServerRequest> {
    /**
     * 反序列化处理请求
     * 根据服务名称获取实现类
     * 利用反射调用方法
     * 封装调用结果
     * 处理响应
     * @param request rpc请求，包含服务名称、方法名称等反射所需的信息
     */
    @Override
    public void handle(HttpServerRequest request) {
        //根据配置文件从序列化器工厂获取
        Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
        System.out.println(request.method() + " " + request.uri());
        //异步处理请求
        request.bodyHandler(buffer -> {
            RpcRequest rpcRequest=null;
            try {
                //获取服务名称、方法名称等请求信息
                rpcRequest = serializer.deserialize(buffer.getBytes(), RpcRequest.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            RpcResponse rpcResponse=new RpcResponse();
            if(rpcRequest==null){
                rpcResponse.setMsg("request is null");
                doResponse(request,rpcResponse,serializer);
                return;
            }
            //根据服务名称，通过反射调用实现方法
            Class<?> service = LocalRegister.getService(rpcRequest.getServiceName());

            try {
                //根据方法名称和请求参数获取实现方法
                Method method = service.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                //调用实现方法获取结果
                Object result = method.invoke(service.getDeclaredConstructor().newInstance(), rpcRequest.getArgs());
                //封装调用结果
                rpcResponse.setData(result);
                rpcResponse.setMsg("success");
                rpcResponse.setClazz(method.getReturnType());//返回结果的类型
                doResponse(request,rpcResponse,serializer);//处理响应
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 序列化调用结果，进入网络传输
     * @param request
     * @param rpcResponse
     * @param serializer
     */
    void doResponse(HttpServerRequest request, RpcResponse rpcResponse, Serializer serializer) {
        HttpServerResponse response = request.response().putHeader("content-type", "application/json");
        try {
            response.end(Buffer.buffer(serializer.serialize(rpcResponse)));//序列化响应
        } catch (IOException e) {
            response.end(Buffer.buffer());
            System.err.println(e.getMessage());
        }

    }
}
