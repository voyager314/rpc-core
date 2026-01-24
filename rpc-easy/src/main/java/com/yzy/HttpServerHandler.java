package com.yzy;

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
        JdkSerializer serializer = new JdkSerializer();
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
            RpcResponse rpcResponce=new RpcResponse();
            if(rpcRequest==null){
                rpcResponce.setMsg("request is null");
                doResponse(request,rpcResponce,serializer);
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
                rpcResponce.setData(result);
                rpcResponce.setMsg("success");
                rpcResponce.setClazz(method.getReturnType());//返回结果的类型
                doResponse(request,rpcResponce,serializer);//处理响应
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
