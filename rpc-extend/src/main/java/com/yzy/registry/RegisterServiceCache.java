package com.yzy.registry;

import com.yzy.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册中心本地缓存
 */
public class RegisterServiceCache {

    //List<ServiceMetaInfo> serviceCache;

    /**
     * 服务缓存，支持多服务缓存
     */
    Map<String, List<ServiceMetaInfo>> serviceCache=new ConcurrentHashMap<>();

    /**
     * 写缓存
     *
     * @param newServiceCache
     * @return
     */
    void writeCache(String serviceKey ,List<ServiceMetaInfo> newServiceCache) {
        serviceCache.put(serviceKey,newServiceCache);
    }

    /**
     * 读缓存
     *
     * @return
     */
    List<ServiceMetaInfo> readCache(String serviceKey) {
        return serviceCache.get(serviceKey);
    }

    /**
     * 清空缓存
     */
    void clearCache(String serviceKey) {
        serviceCache.remove(serviceKey);
    }
}
