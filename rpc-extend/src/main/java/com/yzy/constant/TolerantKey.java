package com.yzy.constant;

/**
 * 容错机制keys
 */
public interface TolerantKey {
    /**
     * 快速失败
     */
    String FAIL_FAST = "failFast";

    /**
     * 静默处理
     */
    String FAIL_SAFE = "failSafe";
}
