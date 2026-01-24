package com.yzy.model;

import com.yzy.constant.RpcConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RpcRequest implements Serializable {
    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数列表
     */
    private Object[] args;
    /**
     * 方法形参类型列表
     *
     */
    private Class<?>[] parameterTypes;
    /**
     * 默认服务版本
     */
    private String serviceVersion= RpcConstant.DEFAULT_SERVICE_VERSION;//默认服务版本
}
