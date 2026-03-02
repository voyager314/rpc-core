package com.yzy.fault.tolerant;

import com.yzy.model.RpcResponse;

import java.util.Map;

/**
 * 快速失败
 */
public class FailFast implements TolerantStrategy{
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        throw new RuntimeException("服务报错",e);
    }
}
