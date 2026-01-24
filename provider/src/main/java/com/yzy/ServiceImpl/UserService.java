package com.yzy.ServiceImpl;

import com.yzy.model.User;
import com.yzy.service.IUserService;

public class UserService implements IUserService {
    @Override
    public User getUserByName(String name) {
        System.out.println("用户名称："+name);
        return new User(name,22);
    }
}
