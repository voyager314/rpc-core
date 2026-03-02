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
                //接口数组，提供需要代理的方法，若实现类实现了多个接口，可指定多个接口
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
