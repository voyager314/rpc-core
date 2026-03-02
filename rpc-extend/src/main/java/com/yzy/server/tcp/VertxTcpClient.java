package com.yzy.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yzy.RpcApplication;
import com.yzy.config.RpcConfig;
import com.yzy.constant.ProtocolConstant;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.protocol.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * tcp客户端
 */
@SuppressWarnings("unchecked")
@Slf4j
public class VertxTcpClient {
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo metaInfo) throws ExecutionException, InterruptedException {
        Vertx vertx = Vertx.vertx();
        //vertx 发送tcp请求
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        NetClient netClient = vertx.createNetClient();

        netClient.connect(metaInfo.getServicePort(), metaInfo.getServiceHost(), result -> {
            if (!result.succeeded()) {
                log.error("failed to connect tcp server: {}", result.cause().getMessage());
                return;
            }
            //发送数据，构造消息
            NetSocket socket = result.result();
            ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
            ProtocolMessage.Header header = new ProtocolMessage.Header();
            header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
            header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            ProtocolMsgSerializerEnum anEnum = ProtocolMsgSerializerEnum.getEnumByName(rpcConfig.getSerializer());
            if (anEnum == null) {
                throw new RuntimeException("不存在该序列化协议");
            }
            header.setSerializer((byte) anEnum.getCode());
            header.setType((byte) ProtocolMsgTypeEnum.REQUEST.getValue());
            //生成全局请求id
            header.setRequestId(IdUtil.getSnowflakeNextId());
            protocolMessage.setHeader(header);
            protocolMessage.setBody(rpcRequest);
            Buffer encode = null;
            try {
                //编码请求
                encode = ProtocolMsgEncoder.encode(protocolMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //发送
            socket.write(encode);

            //接收响应，异步回调
            TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(
                    buffer -> {
                        ProtocolMessage<RpcResponse> responseMessage = null;
                        try {
                            responseMessage = (ProtocolMessage<RpcResponse>) ProtocolMsgDecoder.decode(buffer);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        responseFuture.complete(responseMessage.getBody());
                        //将close执行时机移到响应回调lambda内执行，即responseFuture.complete之后
                        //避免连接到达前被关闭
                        netClient.close();
                    }
            );
            socket.handler(bufferHandlerWrapper);
        });
        //netClient.close();
        return responseFuture.get();
    }
    //public static void main(String[] args) {new VertxTcpClient().start();}
}
