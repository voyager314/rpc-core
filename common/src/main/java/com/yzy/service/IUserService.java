package com.yzy.service;

import com.yzy.model.User;

public interface IUserService {
    public User getUserByName(String name);

    public int add(int input,int num);

    /**
     * 这是服务接口的默认实现，用于测试模拟是否成功
     * 如果调用的方法不是这个说明走的是代理，模拟成功
     * @return
     */
    default int mockGetNum(){
        return 1;
    }
}
