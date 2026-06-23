package com.yzy.server.tcp;

import com.yzy.RpcApplication;
import com.yzy.model.RpcResponse;
import com.yzy.protocol.ProtocolMessage;
import com.yzy.protocol.ProtocolMsgDecoder;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP 连接池（单例）
 * <p>
 * 以 host:port 为 key 管理长连接，实现连接复用和多路复用。
 * 每条连接上注册一个 TcpBufferHandlerWrapper 持续解码响应，
 * 通过 UnresolvedFutureMap 按 requestId 将响应分发到对应的 CompletableFuture。
 * <p>
 * 首期每个 endpoint 维护一条连接（Vert.x event-loop 非阻塞，单连接即可达到高吞吐），
 * 后续可扩展为多连接池。
 */
@Slf4j
public class TcpConnectionPool {

    private static volatile TcpConnectionPool instance;

    /**
     * endpoint(host:port) → 连接 future，value 为 CompletableFuture 以支持连接建立中的并发请求复用
     */
    private final ConcurrentHashMap<String, CompletableFuture<PooledConnection>> connectionMap = new ConcurrentHashMap<>();
    private final NetClient netClient;

    private TcpConnectionPool() {
        Vertx vertx = RpcApplication.getVertx();
        NetClientOptions options = new NetClientOptions()
                .setReconnectAttempts(3)
                .setReconnectInterval(1000);
        this.netClient = vertx.createNetClient(options);
    }

    public static TcpConnectionPool getInstance() {
        if (instance == null) {
            synchronized (TcpConnectionPool.class) {
                if (instance == null) {
                    instance = new TcpConnectionPool();
                }
            }
        }
        return instance;
    }

    /**
     * 获取到指定 endpoint 的连接
     * <p>
     * 三种情况：
     * 1. 已有活跃连接 → 直接复用
     * 2. 连接正在建立中 → 返回同一个 future，多个并发请求共享等待
     * 3. 无连接或已失效 → 新建连接
     * <p>
     * compute 保证同一个 key 的操作原子性，避免重复建连
     */
    public CompletableFuture<PooledConnection> getConnection(String host, int port) {
        String key = host + ":" + port;
        return connectionMap.compute(key, (k, existing) -> {
            if (existing != null && !existing.isCompletedExceptionally()) {
                if (existing.isDone()) {
                    PooledConnection conn = existing.join();
                    if (conn.isActive()) {
                        return existing;
                    }
                    log.info("连接 {} 已失效，重新建立", key);
                } else {
                    // 连接正在建立中，返回同一个 future，多个并发请求共享等待
                    return existing;
                }
            }
            return createConnection(host, port);
        });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<PooledConnection> createConnection(String host, int port) {
        CompletableFuture<PooledConnection> future = new CompletableFuture<>();
        String key = host + ":" + port;

        netClient.connect(port, host, result -> {
            if (!result.succeeded()) {
                log.error("连接 {} 失败: {}", key, result.cause().getMessage());
                connectionMap.remove(key);
                future.completeExceptionally(result.cause());
                return;
            }

            NetSocket socket = result.result();
            UnresolvedFutureMap pendingRequests = new UnresolvedFutureMap();
            PooledConnection conn = new PooledConnection(socket, pendingRequests);

            // 核心：注册响应分发 handler
            // TcpBufferHandlerWrapper 的 RecordParser 会持续解码同一连接上的所有响应，
            // 每解码出一条完整响应，就按 requestId 从 UnresolvedFutureMap 中找到对应 future 并 complete
            TcpBufferHandlerWrapper handler = new TcpBufferHandlerWrapper(buffer -> {
                try {
                    ProtocolMessage<RpcResponse> responseMessage =
                            (ProtocolMessage<RpcResponse>) ProtocolMsgDecoder.decode(buffer);
                    long requestId = responseMessage.getHeader().getRequestId();
                    pendingRequests.resolve(requestId, responseMessage.getBody());
                } catch (IOException e) {
                    log.error("解码响应失败", e);
                }
            });
            socket.handler(handler);

            // 连接关闭时 fail 掉所有 pending futures 并从池中移除
            socket.closeHandler(v -> {
                log.info("连接 {} 已关闭", key);
                pendingRequests.failAll(new IOException("连接已关闭: " + key));
                connectionMap.remove(key);
            });

            socket.exceptionHandler(err -> {
                log.error("连接 {} 异常: {}", key, err.getMessage());
                pendingRequests.failAll(err);
                connectionMap.remove(key);
                socket.close();
            });

            log.info("连接 {} 建立成功", key);
            future.complete(conn);
        });

        return future;
    }

    public void close() {
        connectionMap.forEach((key, future) -> {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                PooledConnection conn = future.join();
                conn.close();
            }
        });
        connectionMap.clear();
        netClient.close();
    }

    /**
     * 池化连接，持有 socket 和该连接上的 pending 请求映射
     */
    @Getter
    public static class PooledConnection {
        private final NetSocket socket;
        private final UnresolvedFutureMap pendingRequests;

        PooledConnection(NetSocket socket, UnresolvedFutureMap pendingRequests) {
            this.socket = socket;
            this.pendingRequests = pendingRequests;
        }

        public boolean isActive() {
            return socket != null && !socket.writeHandlerID().isEmpty();
        }

        public void close() {
            pendingRequests.failAll(new IOException("连接主动关闭"));
            socket.close();
        }
    }
}
