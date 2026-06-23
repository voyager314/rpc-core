package com.yzy;

import com.yzy.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@EnableRpc(needServer = false)
public class ExamConsumerSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamConsumerSpringbootApplication.class, args);
    }

}
