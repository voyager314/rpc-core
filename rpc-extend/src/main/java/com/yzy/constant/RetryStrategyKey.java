package com.yzy.constant;

/**
 * 重试策略
 */
public interface RetryStrategyKey {
    /**
     * 不重试
     */
    String NO="no";

    /**
     * 固定间隔重试
     */
    String FIXED_INTERVAL="fixedInterval";
}
