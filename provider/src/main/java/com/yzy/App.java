package com.yzy;

import com.yzy.ServiceImpl.UserService;
import com.yzy.registry.LocalRegister;
import com.yzy.server.VertxHttpServer;
import com.yzy.service.IUserService;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {
        //提供服务
        //服务名称为服务接口类名，其中注册的服务实例，即服务真正提供者则为实现类！
        LocalRegister.putService(IUserService.class.getName(), UserService.class);
        new VertxHttpServer().start(8081);
    }
}
