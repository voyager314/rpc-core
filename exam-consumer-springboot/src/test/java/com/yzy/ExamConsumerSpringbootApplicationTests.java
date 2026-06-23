package com.yzy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ExamConsumerSpringbootApplicationTests {
    @Autowired TestConsumer testConsumer;

    @Test
    void contextLoads() {
        testConsumer.test();
    }

    @Test
    void test1(){
        testConsumer.test2();
    }


}
