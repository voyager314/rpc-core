package com.yzy.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yzy.RpcApplication;
import com.yzy.config.RpcConfig;
import com.yzy.constant.ProtocolConstant;
import com.yzy.model.RpcRequest;
import com.yzy.model.RpcResponse;
import com.yzy.model.ServiceMetaInfo;
import com.yzy.protocol.*;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * TCP 客户端（多路复用版本）
 * <p>
 * 通过 TcpConnectionPool 复用到同一 endpoint 的 TCP 连接，
 * 利用协议头中的 requestId 实现同一连接上的多路复用：
 * 多个并发请求共享一条连接，响应按 requestId 分发到各自的 CompletableFuture。
 */
@Slf4j
public class VertxTcpClient {

    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo metaInfo) throws ExecutionException, InterruptedException {
        // 构造协议消息
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
        long requestId = IdUtil.getSnowflakeNextId();
        header.setRequestId(requestId);
        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);

        Buffer encode;
        try {
            encode = ProtocolMsgEncoder.encode(protocolMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 从连接池获取复用连接（同一 endpoint 共享一条 TCP 连接）
        TcpConnectionPool pool = TcpConnectionPool.getInstance();
        TcpConnectionPool.PooledConnection conn = pool
                .getConnection(metaInfo.getServiceHost(), metaInfo.getServicePort())
                .get();

        // 先注册 requestId → future 映射，再发送请求
        // 保证响应到达时 UnresolvedFutureMap 中已有对应的 future
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
        conn.getPendingRequests().addPending(requestId, responseFuture);
        conn.getSocket().write(encode);

        //由于Vertx的请求处理器是异步的，这里使用CompletebleFuture转异步为同步
        //阻塞等待响应
        return responseFuture.get();
    }
}
