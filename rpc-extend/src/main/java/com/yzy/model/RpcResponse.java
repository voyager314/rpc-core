package com.yzy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcResponse implements Serializable {
    /**
     * 请求结果信息
     */
    private String msg;
    /**
     * 请求结果
     */
    private Object data;
    /**
     * 返回值类型信息
     */
    private Class<?> clazz;
}
