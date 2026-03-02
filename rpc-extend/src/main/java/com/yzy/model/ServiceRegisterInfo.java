package com.yzy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务注册信息
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRegisterInfo<T> {
    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 服务实现类
     */
    private Class<T> implClass;
}
