package com.bkjk.platform.lock.test;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 20:47
 **/
@Configuration
public class Config {

    @Bean
    public RedissonClient redissonClient(){
        return Redisson.create();
    }
}
