package com.yzy.registry;

import com.yzy.util.SPILoader;

/**
 * 注册器工厂
 * 工厂模式+单例模式
 */
public class RegisterFactory {
    static {
        SPILoader.load(Register.class);
    }

    /**
     * 获取注册器实例
     * @param key
     * @return
     */
    public static Register getInstance(String key){
        return SPILoader.getInstance(Register.class, key);
    }
}
