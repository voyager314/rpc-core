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
public class RpcResponse implements Serializable {
    private String msg;
    private Object data;//返回值
    private Class<?> clazz;//返回值的类型信息
}
