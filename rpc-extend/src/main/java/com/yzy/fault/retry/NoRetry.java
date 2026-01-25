package com.yzy.fault.retry;

import com.yzy.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 重试策略不重试
 */
public class NoRetry implements RetryStrategy{
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        return callable.call();
    }
}
