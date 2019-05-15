package com.bkjk.platform.lock.autoconfigure;

import com.bkjk.platform.lock.redisson.RedissonLockFactory;
import com.bkjk.platform.lock.redisson.RedissonLockHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.bkjk.platform.lock.autoconfigure.LockAutoConfiguration.DEFAULT_LOCK_FACTORY_BEAN;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 9:32
 **/
@ConditionalOnProperty(value =  LockConfiguration.PREFIX+".enable",matchIfMissing = true)
@EnableConfigurationProperties(LockConfiguration.class)
@AutoConfigureBefore(LockAutoConfiguration.class)
public class RedissonLockAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(name = DEFAULT_LOCK_FACTORY_BEAN)
    @ConditionalOnBean(RedissonClient.class)
    public static class LockFactoryAutoConfiguration{

        @Bean
        public RedissonLockHandler redissonLockHandler(){
            return new RedissonLockHandler();
        }

        @Bean(name = DEFAULT_LOCK_FACTORY_BEAN)
        public RedissonLockFactory redissonLockFactory(RedissonClient redissonClient,RedissonLockHandler redissonLockHandler){
            return new RedissonLockFactory(redissonClient,redissonLockHandler);
        }
    }

}
