package com.yzy;

import com.yzy.annotation.EnableRpcService;
import com.yzy.model.User;
import com.yzy.service.IUserService;
import org.springframework.stereotype.Service;

@Service
@EnableRpcService(interfaceClass = IUserService.class)
public class UserService implements IUserService {
    @Override
    public User getUserByName(String name) {
        return new User(name,21);
    }

    @Override
    public int add(int input, int num) {
        return input + num;
    }
}
