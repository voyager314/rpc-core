package com.yzy.loadbalancer;

import com.yzy.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器
 */
public class RoundRobinLB implements LoadBalancer {
    //JUC的原子计数器，避免并发冲突
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ServiceMetaInfo select(Map<String, Object> params, List<ServiceMetaInfo> services) {
        if(services == null || services.isEmpty())return null;
        int size=services.size();
        //只有一个无需轮询
        if(size==1)return services.get(0);
        //使用&去掉符号位，避免溢出时出现负数导致数组越界
        int index = counter.getAndIncrement() & Integer.MAX_VALUE;
        return services.get(index%size);
    }
}
