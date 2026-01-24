package com.yzy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRegister {
    private static final Map<String,Class<?>>map=new ConcurrentHashMap<String,Class<?>>();
    //使用ConcurrentHashMap通过反射获取目标服务的实现类
    public static Class<?> getService(String service){
        return map.get(service);
    }

    public static void putService(String service,Class<?> serviceClass){
        map.put(service,serviceClass);
    }

    public static void removeService(String service){
        map.remove(service);
    }
}
