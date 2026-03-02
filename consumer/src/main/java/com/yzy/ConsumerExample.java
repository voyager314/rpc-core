package com.yzy;

import com.yzy.bootstrap.ConsumerBootStrap;
import com.yzy.model.User;
import com.yzy.proxy.ServiceProxyFactory;
import com.yzy.service.IUserService;

/**
 * 简单服务消费者示例
 */
public class ConsumerExample {
    public static void main(String[] args) {
        //初始化RPC框架
        ConsumerBootStrap.init();

        IUserService userService = ServiceProxyFactory.getProxy(IUserService.class);
        User user = userService.getUserByName("飞鱼");
        if (user == null) {
            System.out.println("服务调用失败！");
        }
        System.out.println(user);
        System.out.println("服务调用成功~");
        //模拟服务
        IUserService mockProxy = ServiceProxyFactory.getMockProxy(IUserService.class);
        int test=2;
        test= mockProxy.mockGetNum();//这里重定向到代理对象的invoke方法，输出0
        System.out.println(test);
    }
}
