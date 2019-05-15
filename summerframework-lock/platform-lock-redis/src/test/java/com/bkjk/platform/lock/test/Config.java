package com.bkjk.platform.lock.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 20:47
 **/
@Configuration
public class Config {

    @Bean
    public RedisTemplate redisTemplate(){
        RedisTemplate redisTemplate=new RedisTemplate();
        redisTemplate.setConnectionFactory(new JedisConnectionFactory());
        return redisTemplate;
    }

}
