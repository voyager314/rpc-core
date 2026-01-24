package com.yzy.service;

import com.yzy.model.User;

public interface IUserService {
    public User getUserByName(String name);

    default int mockGetNum(){
        return 1;
    }
}
