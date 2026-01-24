package com.yzy.server.tcp;

import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.protocol.ProtocolMessage;
import com.yzy.protocol.ProtocolMsgDecoder;
import com.yzy.protocol.ProtocolMsgEncoder;
import com.yzy.protocol.ProtocolMsgTypeEnum;
import com.yzy.registry.LocalRegister;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.lang.reflect.Method;

public class TcpServerHandler implements Handler<NetSocket> {
    @Override
    public void handle(NetSocket netSocket) {
        netSocket.handler(buffer -> {
            ProtocolMessage<RpcRequest> protocolMessage = null;
            try {
                protocolMessage = (ProtocolMessage<RpcRequest>) ProtocolMsgDecoder.decode(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            RpcRequest rpcRequest = protocolMessage.getBody();
            RpcResponse rpcResponse = new RpcResponse();
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
            } catch (Exception e) {
                rpcResponse.setMsg(e.getMessage());
                throw new RuntimeException(e);
            }
            ProtocolMessage.Header header = protocolMessage.getHeader();
            header.setType((byte) ProtocolMsgTypeEnum.RESPONSE.getValue());
            ProtocolMessage<RpcResponse> responseProtocolMessage = new ProtocolMessage<>(header, rpcResponse);
            try {
                Buffer encode = ProtocolMsgEncoder.encode(responseProtocolMessage);
                netSocket.write(encode);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
