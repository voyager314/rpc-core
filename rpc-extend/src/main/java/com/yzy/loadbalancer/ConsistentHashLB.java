package com.yzy.loadbalancer;

import com.google.common.hash.Hashing;
import com.yzy.model.ServiceMetaInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 一致性哈希负载均衡器
 */
public class ConsistentHashLB implements LoadBalancer {
    /**
     * 一致性哈希环，存放虚拟节点
     */
    private final TreeMap<Long,ServiceMetaInfo>virtualNodes=new TreeMap<>();

    /**
     * 虚拟节点数量
     */
    private static final int VIRTUAL_NODE_NUM=100;

    //synchronized避免并发冲突
    @Override
    public synchronized ServiceMetaInfo select(Map<String, Object> params, List<ServiceMetaInfo> services) {
        if(services==null || services.isEmpty())return null;
        //每次重新构建环以应对节点变化
        virtualNodes.clear();
        for (ServiceMetaInfo service : services) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String s = String.format("%s#%d", service.getServiceAddr(), i);
                //这里使用guava的MurmurHash
                long hash = Hashing.murmur3_128().hashString(s, StandardCharsets.UTF_8).asLong();
                virtualNodes.put(hash, service);
            }
        }
        long requestHash = Hashing.murmur3_128().hashString(params.toString(), StandardCharsets.UTF_8).asLong();
        Map.Entry<Long, ServiceMetaInfo> entry = virtualNodes.ceilingEntry(requestHash);
        if(entry==null)return virtualNodes.firstEntry().getValue();
        return entry.getValue();
    }

}
