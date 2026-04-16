package com.yzy;

import com.yzy.config.RegistryConfig;
import com.yzy.config.RpcConfig;
import com.yzy.constant.RpcConstant;
import com.yzy.registry.Register;
import com.yzy.registry.RegisterFactory;
import com.yzy.util.ConfigUtil;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcApplication {
    private static volatile RpcConfig rpcConfig;//volatile多线程可见性
    private static volatile Vertx vertx;

    /**
     * 全局共享 Vertx 实例（双检锁单例）
     * 避免每次请求都创建新的 Vertx，导致线程池爆炸
     */
    public static Vertx getVertx() {
        if (vertx == null) {
            synchronized (RpcApplication.class) {
                if (vertx == null) {
                    vertx = Vertx.vertx();
                    Runtime.getRuntime().addShutdownHook(
                            new Thread(() -> vertx.close(), "vertx-shutdown")
                    );
                }
            }
        }
        return vertx;
    }

    /**
     * 自定义rpc配置
     * @param newConfig
     */
    public static void setCustomConfig(RpcConfig newConfig) {
        synchronized (RpcApplication.class) {
            if (rpcConfig != null) {
                log.warn("rpc config is already set!");
            }
            rpcConfig = newConfig;
            log.info("custom config: {}", rpcConfig);
        }
    }

    /**
     * 配置文件初始化
     */
    private static void init(){
        try {
            rpcConfig= ConfigUtil.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
            if(rpcConfig==null){
                rpcConfig=new RpcConfig();
            }
        } catch (Exception e) {
            //配置加载失败，使用默认配置
            rpcConfig = new RpcConfig();
        }
        log.info("RpcApplication init, rpcConfig:{}", rpcConfig);
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Register register = RegisterFactory.getInstance(registryConfig.getRegistry());
        register.init(registryConfig);
        log.info("Register init, RegistryConfig:{}", registryConfig);
        //使用shutdownhook在jvm退出前执行节点主动下线操作
        Runtime.getRuntime().addShutdownHook(new Thread(register::destroy));
    }

    /**
     * 双检锁单例模式
     * 获取配置
     * @return
     */
    public static RpcConfig getRpcConfig(){
        if(rpcConfig==null){
            synchronized (RpcApplication.class){
                if(rpcConfig==null){
                    init();
                }
            }
        }
        return rpcConfig;
    }
}
