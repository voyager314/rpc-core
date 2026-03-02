package com.yzy.fault.tolerant;

import com.yzy.util.SPILoader;

/**
 * 容错机制工厂
 */
public class TolerantFactory {
    static {
        SPILoader.load(TolerantStrategy.class);
    }

    public static TolerantStrategy getInstance(String key){
        return SPILoader.getInstance(TolerantStrategy.class, key);
    }
}
