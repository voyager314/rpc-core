package com.yzy.loadbalancer;

import com.yzy.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 随机负载均衡器
 */
public class RandomLB implements LoadBalancer {
    @Override
    public synchronized ServiceMetaInfo select(Map<String, Object> params, List<ServiceMetaInfo> services) {
        if(services == null || services.isEmpty())return null;
        int size = services.size();
        if(size==1)return services.get(0);
        Random random = new Random();
        return services.get(random.nextInt(size));
    }
}
