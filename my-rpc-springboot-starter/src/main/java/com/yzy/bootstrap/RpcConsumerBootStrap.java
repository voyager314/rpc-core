package com.yzy.bootstrap;

import com.yzy.annotation.EnableRefrence;
import com.yzy.proxy.ServiceProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

public class RpcConsumerBootStrap implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        Field[] fields = beanClass.getDeclaredFields();
        //遍历对象所有属性
        for (Field field : fields) {
            EnableRefrence annotation = field.getAnnotation(EnableRefrence.class);
            if (annotation != null) {
                Class<?> interfaceClass = annotation.interfaceClass();
                if(interfaceClass==void.class){
                    interfaceClass=field.getType();
                }
                //为属性生成代理对象
                Object proxy = ServiceProxyFactory.getProxy(interfaceClass);
                field.setAccessible(true);
                try {
                    //将该字段所指向的对象替换为代理对象
                    field.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
