package com.yzy.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.Setting;
import cn.hutool.setting.dialect.Props;

/**
 * 读取配置的工具类
 */
public class ConfigUtil {
    /**
     * 加载配置对象
     *
     * @param tClass
     * @param prefix
     * @param <T>
     * @return
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix) {
        return loadConfig(tClass, prefix, null);
    }

    /**
     * 加载配置对象，支持区分环境
     *
     * @param tClass
     * @param prefix
     * @param environment
     * @param <T>
     * @return
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix, String environment) {
        StringBuilder sb = new StringBuilder("application");
        if (StrUtil.isNotBlank(environment)) {
            sb.append("-").append(environment);
        }
        sb.append(".properties");
        try {
            Props props = new Props(sb.toString());
            props.autoLoad(true);//配置文件变更时自动加载
            return props.toBean(tClass, prefix);
        } catch (Exception e) {
            //若没有properties文件，则加载yml/yaml
            Setting setting = loadSettings(environment);
            if (setting != null) {
                setting.autoLoad(true);
                return setting.toBean(tClass);
            }
            return null;
        }
    }

    /**
     * 加载yml/yaml配置
     * @param env
     * @return
     */
    public static Setting loadSettings(String env){
        StringBuilder sb = new StringBuilder("application");
        if (StrUtil.isNotBlank(env)) {
            sb.append("-").append(env);
        }
        sb.append(".yml");
        Setting setting = null;
        try {
            setting=new Setting(sb.toString());
        } catch (Exception e) {
            sb.replace(sb.lastIndexOf(".yml"),sb.length(),".yaml");
            try {
                setting=new Setting(sb.toString());
            } catch (Exception ex) {
                return null;//若两种格式都没有，返回null
            }
        }
        return setting;
    }
}
