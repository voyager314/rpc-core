package com.yzy.model;

import cn.hutool.core.util.StrUtil;
import com.yzy.constant.RpcConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务注册元信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceMetaInfo {
    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务版本号
     */
    private String serviceVersion = RpcConstant.DEFAULT_SERVICE_VERSION;

    /**
     * 服务域名
     */
    private String serviceHost;

    /**
     * 服务端口号
     */
    private Integer servicePort;

    /**
     * 服务分组（暂未实现）
     */
    private String serviceGroup = "default";

    /**
     * 获取服务键名
     * @return
     */
    public String getServiceKey(){
        return String.format("%s:%s", serviceName, serviceVersion);
    }

    /**
     * 获取服务节点名
     * @return
     */
    public String getServiceNodeKey(){
        return String.format("%s/%s:%s", getServiceKey(), serviceHost, servicePort);
    }

    /**
     * 获取服务地址
     * @return
     */
    public String getServiceAddr(){
        if(!StrUtil.contains("http", serviceHost)){
            return String.format("http//:%s:%s", serviceHost, servicePort);
        }
        return String.format("%s:%s", serviceHost, servicePort);
    }
}
