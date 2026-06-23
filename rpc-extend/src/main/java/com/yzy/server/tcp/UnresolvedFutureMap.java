package com.yzy.server.tcp;

import com.yzy.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多路复用请求-响应调度器
 * <p>
 * 维护 requestId → CompletableFuture 的映射关系，
 * 同一条 TCP 连接上的多个并发请求各自持有不同 requestId，
 * 响应到达时按 requestId 分发到对应的 CompletableFuture，实现多路复用。
 */
@Slf4j
public class UnresolvedFutureMap {

    /**
     * requestId → 等待响应的 CompletableFuture，线程安全
     */
    private final ConcurrentHashMap<Long, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 发送请求前注册，将 requestId 与对应的 future 关联
     */
    public void addPending(long requestId, CompletableFuture<RpcResponse> future) {
        pendingRequests.put(requestId, future);
    }

    /**
     * 收到响应时调用，按 requestId 查找并 complete 对应的 future
     * 使用 remove 保证同一个 requestId 只会被 resolve 一次
     */
    public void resolve(long requestId, RpcResponse response) {
        CompletableFuture<RpcResponse> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("收到未知 requestId 的响应: {}", requestId);
        }
    }

    /**
     * 超时或主动取消时移除并取消对应 future
     */
    public void removePending(long requestId) {
        CompletableFuture<RpcResponse> future = pendingRequests.remove(requestId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    /**
     * 连接断开/异常时，将所有未完成的 pending 请求标记为异常
     */
    public void failAll(Throwable cause) {
        for (Map.Entry<Long, CompletableFuture<RpcResponse>> entry : pendingRequests.entrySet()) {
            entry.getValue().completeExceptionally(cause);
        }
        pendingRequests.clear();
    }

}
