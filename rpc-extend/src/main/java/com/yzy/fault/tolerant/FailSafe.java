package com.yzy.fault.tolerant;

import com.yzy.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 静默处理
 */
@Slf4j
public class FailSafe implements TolerantStrategy{
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        log.error("静默处理异常",e);
        return new RpcResponse();
    }
}
