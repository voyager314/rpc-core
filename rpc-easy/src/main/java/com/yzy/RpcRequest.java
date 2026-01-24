package com.yzy;

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
    private String serviceName;//服务名称
    private String methodName;//方法名
    private Object[] args;//参数列表
    private Class<?>[] parameterTypes;//参数类型列表
}
