package com.yzy.fault.retry;

import com.github.rholder.retry.*;
import com.yzy.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FixedIntervalRetry implements RetryStrategy{
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class)//发生异常时重试
                .withWaitStrategy(WaitStrategies.fixedWait(3L, TimeUnit.SECONDS))//每3秒重试一次
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))//重试超过3次就停止
                .withRetryListener(new RetryListener() {
                    //重试监听，打印当前重试次数
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        log.info("重试次数 {}", attempt.getAttemptNumber());
                    }
                })
                .build();
        return retryer.call(callable);
    }
}
