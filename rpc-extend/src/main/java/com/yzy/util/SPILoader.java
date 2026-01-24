package com.yzy.util;

import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
@Slf4j
public class SPILoader {
    /**
     * 存储已加载的类：接口名 =>（key => 实现类）
     */
    private static final Map<String, Map<String, Class<?>>> loaderMap = new ConcurrentHashMap<>();

    /**
     * 对象实例缓存（避免重复 new），类路径 => 对象实例，单例模式
     */
    private static final Map<String, Object> instanceCache = new ConcurrentHashMap<>();

    /**
     * 系统 SPI 目录
     */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";

    /**
     * 用户自定义 SPI 目录
     */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";

    /**
     * 扫描路径
     */
    private static final String[] SCAN_DIRS = new String[]{RPC_SYSTEM_SPI_DIR, RPC_CUSTOM_SPI_DIR};

    /**
     * 动态加载的类列表
     */
    private static final List<Class<?>> LOAD_CLASS_LIST = List.of(Serializer.class);

    /**
     * 获取某个接口的实例
     * @param clazz
     * @param key
     * @return
     * @param <T>
     */
    public static  <T> T getInstance(Class<?> clazz,String key){
        String name = clazz.getName();
        Map<String, Class<?>> map = loaderMap.get(name);
        if(map == null){
            throw new RuntimeException(String.format("SPILoader 未加载 %s 类型", name));
        }
        if(!map.containsKey(key)){
            throw new RuntimeException(String.format("%s 不存在 key=%s 的类型",name,key));
        }
        Class<?> implClass = map.get(key);
        String implClassName = implClass.getName();
        if(!instanceCache.containsKey(implClassName)){
            try {
                instanceCache.put(implClassName, implClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(String.format("%s 实例化失败",implClassName),e);
            }
        }
        return (T)instanceCache.get(implClassName);
    }

    /**
     * 加载某个类
     * @param loadClass
     * @return
     */
    public static void load(Class<?> loadClass){
        String name = loadClass.getName();
        Map<String,Class<?>>map=new HashMap<>();
        for (String dir : SCAN_DIRS) {
            List<URL> resources = ResourceUtil.getResources(dir + name);
            for (URL resource : resources) {
                InputStreamReader reader = null;
                try {
                    reader = new InputStreamReader(resource.openStream());
                    new BufferedReader(reader).lines().forEach(line -> {
                        String[] strings = line.split("=");
                        if(strings.length == 2){
                            String key = strings[0];
                            String value = strings[1];
                            try {
                                map.put(key,Class.forName(value));
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        loaderMap.put(name, map);
    }
}
