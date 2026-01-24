package com.yzy.proxy;



import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

@Slf4j
public class MockServiceProxy implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      log.info("mock invoke {}",method.getName());
      return getDefaultObj(method.getReturnType());
    }

    /**
     * 生成指定类型的默认值对象
     * @param type
     * @return
     */
    private Object getDefaultObj(Class<?> type) {
        if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return 0L;
        } else if (type == boolean.class) {
            return false;
        } else if (type == float.class) {
            return 0.0f;
        } else if (type == double.class) {
            return 0.0d;
        } else if (type == char.class) {
            return '\0';
        } else if (type == byte.class) {
            return (byte) 0;
        } else if (type == short.class) {
            return (short) 0;
        } else return null;
    }
}
