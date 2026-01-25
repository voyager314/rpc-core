package com.yzy.loadbalancer;

import com.yzy.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡器（消费者使用）
 */
public interface LoadBalancer {
    /**
     * 选择服务调用
     * @param params
     * @param services
     * @return
     */
    public ServiceMetaInfo select(Map<String,Object> params, List<ServiceMetaInfo> services);
}
