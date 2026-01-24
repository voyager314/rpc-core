package com.yzy.proxy;

import java.lang.reflect.Proxy;

/**
 * 工厂模式动态生产代理对象
 */
@SuppressWarnings("unchecked")
public class ServiceProxyFactory {
    public static <T> T getProxy(Class<T> serviceClass){
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new ServiceProxy()
        );
    }

    public static <T> T getMockProxy(Class<T> mockClass){
        return (T) Proxy.newProxyInstance(
                mockClass.getClassLoader(),
                new Class[]{mockClass},
                new MockServiceProxy()
        );
    }
}
