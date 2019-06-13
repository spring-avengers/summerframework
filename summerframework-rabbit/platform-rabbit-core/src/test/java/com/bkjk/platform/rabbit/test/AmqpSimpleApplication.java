
package com.bkjk.platform.rabbit.test;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/2/22 13:37
 **/
@SpringBootApplication
@EnableScheduling
public class AmqpSimpleApplication {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        System.err.println(rabbitTemplate);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AmqpSimpleApplication.class, args);
    }

}
