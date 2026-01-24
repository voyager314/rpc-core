package com.yzy.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.yzy.config.RegistryConfig;
import com.yzy.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class EtcdRegister implements Register {
    private Client client;
    private KV kvClient;
    private static final String rootPath = "/rpc/";
    /**
     * 本地注册的节点key集合，用于节点续约
     */
    private final Set<String> localKeySet = new HashSet<>();

    /**
     * 服务提供者信息的本地缓存
     */
    private final RegisterServiceCache localCache = new RegisterServiceCache();

    /**
     * 正在监听的 key 集合
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    /**
     * 初始化
     *
     * @param registryConfig
     */
    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = client.getKVClient();
        heartBeat();
    }

    /**
     * 注册服务
     *
     * @param serviceMetaInfo
     * @throws Exception
     */
    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        //创建租约
        Lease lease = client.getLeaseClient();
        //设置30s租约
        long id = lease.grant(30).get().getID();
        //设置要存储的键值对
        String path = rootPath + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(path, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);
        PutOption putOption = PutOption.builder().withLeaseId(id).build();
        //把键值对和租约关联
        kvClient.put(key, value, putOption).get();
        //添加一个节点信息到本地缓存
        localKeySet.add(path);
    }

    /**
     * 注销服务
     *
     * @param serviceMetaInfo
     */
    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        kvClient.delete(ByteSequence.from(rootPath + serviceMetaInfo.getServiceNodeKey(), StandardCharsets.UTF_8));
        localKeySet.remove(rootPath + serviceMetaInfo.getServiceNodeKey());
    }

    /**
     * 服务发现
     *
     * @param serviceKey 服务键名
     * @return
     */
    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        //优先查本地缓存，若不为空，则直接返回
        List<ServiceMetaInfo> localRcords = localCache.readCache();
        if (!CollUtil.isEmpty(localRcords)) {
            return localRcords;
        }
        String prefix = rootPath + serviceKey + "/";
        GetOption getOption = GetOption.builder().isPrefix(true).build();
        List<KeyValue> kvs = null;
        try {
            kvs = kvClient.get(ByteSequence.from(prefix, StandardCharsets.UTF_8), getOption).get().getKvs();
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
        List<ServiceMetaInfo> serviceMetaInfos = kvs.stream().map(kv -> {
                    String key = kv.getValue().toString(StandardCharsets.UTF_8);
                    //监听服务变化
                    watch(key);
                    return JSONUtil.toBean(key, ServiceMetaInfo.class);
                })
                .toList();
        //写入本地缓存
        localCache.writeCache(serviceMetaInfos);
        return serviceMetaInfos;
    }

    /**
     * 心跳检测，若节点过期了还未续约则视为宕机，直接删除
     */
    @Override
    public void heartBeat() {
        //10s续约一次，注意续约间隔要小于节点TTL
        CronUtil.schedule("*/10 * * * * *", new Task() {
            @Override
            public void execute() {
                for (String k : localKeySet) {
                    //遍历并获取所有节点
                    List<KeyValue> kvs = null;
                    try {
                        kvs = kvClient.get(ByteSequence.from(k, StandardCharsets.UTF_8))
                                .get()
                                .getKvs();
                        //若为空，只能重启该节点
                        if (CollUtil.isEmpty(kvs)) continue;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    KeyValue keyValue = kvs.get(0);
                    String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                    ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                    try {
                        //续约
                        register(serviceMetaInfo);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        //秒级定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 服务监听方法，消费者专用
     * 用于获取etcd的服务提供者节点下线通知
     * 更新消费者本地缓存
     *
     * @param serviceNodeKey
     */
    @Override
    public void watch(String serviceNodeKey) {
        Watch watchClient = client.getWatchClient();
        if (watchingKeySet.add(serviceNodeKey)) {
            //只对第一次出现的节点添加监听，原本就存在的节点监听依旧有效
            watchClient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8),
                    response -> {
                        for (WatchEvent event : response.getEvents()) {
                            if (event.getEventType().equals(WatchEvent.EventType.DELETE))
                                localCache.clearCache();
                        }
                    });
        }
    }

    /**
     * 项目关闭后释放资源
     */
    @Override
    public void destroy() {
        log.info("当前节点下线");
        for (String key : localKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }

}
