package com.yzy.fault.retry;

import com.yzy.util.SPILoader;

/**
 * 重试策略工厂
 */
public class RetryStrategyFactory {
    static {
        SPILoader.load(RetryStrategy.class);
    }

    /**
     * 获取重试策略
     * @param key
     * @return
     */
    public static RetryStrategy getInstance(String key){
        return SPILoader.getInstance(RetryStrategy.class, key);
    }
}
