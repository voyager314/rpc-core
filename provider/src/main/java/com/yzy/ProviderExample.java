package com.yzy;

import com.yzy.ServiceImpl.UserService;
import com.yzy.bootstrap.ProviderBootStrap;
import com.yzy.model.ServiceRegisterInfo;
import com.yzy.service.IUserService;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单提供者示例
 */
public class ProviderExample {
    public static void main(String[] args) throws Exception {
        //RpcApplication.setCustomConfig(null);自定义配置，可选

       List<ServiceRegisterInfo<?>> infos=new ArrayList<>();
        ServiceRegisterInfo info = new ServiceRegisterInfo(IUserService.class.getName(), UserService.class);
        infos.add(info);
        ProviderBootStrap.init(infos);
    }
}
