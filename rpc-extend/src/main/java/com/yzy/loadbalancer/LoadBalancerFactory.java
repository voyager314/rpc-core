package com.yzy.loadbalancer;

import com.yzy.util.SPILoader;

public class LoadBalancerFactory {
    static {
        SPILoader.load(LoadBalancer.class);
    }

    /**
     * 获取负载均衡器实例
     * @param key
     * @return
     */
    public static LoadBalancer getInstance(String key){
        return SPILoader.getInstance(LoadBalancer.class, key);
    }
}
