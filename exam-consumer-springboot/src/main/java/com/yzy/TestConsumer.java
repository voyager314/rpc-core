package com.yzy;

import com.yzy.annotation.EnableRefrence;
import com.yzy.model.User;
import com.yzy.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class TestConsumer {
    @EnableRefrence(interfaceClass = IUserService.class)
    private IUserService userService;
    public void test(){
        User user = null;
        user=userService.getUserByName("mike");
        if(user==null){
            System.err.println("服务调用失败");
        }else System.out.println(user);
    }
}
