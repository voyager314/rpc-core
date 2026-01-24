package com.yzy.util;

/**
 * 序列化器工厂
 * 工厂模式+单例模式
 */
public class SerializerFactory {
    static {
        SPILoader.load(Serializer.class);
    }

    /**
     * 获取序列化器实例
     * @param key
     * @return
     */
    public static Serializer getInstance(String key){
        return SPILoader.getInstance(Serializer.class, key);
    }
}
