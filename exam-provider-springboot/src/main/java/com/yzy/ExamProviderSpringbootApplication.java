package com.yzy;

import com.yzy.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableRpc(needServer = true)
public class ExamProviderSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamProviderSpringbootApplication.class, args);
    }

}
