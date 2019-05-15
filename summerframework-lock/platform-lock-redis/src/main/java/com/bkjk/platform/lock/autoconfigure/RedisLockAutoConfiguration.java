package com.bkjk.platform.lock.autoconfigure;

import com.bkjk.platform.lock.redis.RedisLockFactory;
import com.bkjk.platform.lock.redis.RedisLockHandler;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.Jedis;

import static com.bkjk.platform.lock.autoconfigure.LockAutoConfiguration.DEFAULT_LOCK_FACTORY_BEAN;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/8 9:32
 **/
@ConditionalOnProperty(value =  LockConfiguration.PREFIX+".enable",matchIfMissing = true)
@EnableConfigurationProperties(LockConfiguration.class)
@AutoConfigureBefore(LockAutoConfiguration.class)
public class RedisLockAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(name = DEFAULT_LOCK_FACTORY_BEAN)
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnClass(Jedis.class)
    public static class LockFactoryAutoConfiguration{

        @Bean
        public RedisLockHandler redisLockHandler(){
            return new RedisLockHandler();
        }

        @Bean(name = DEFAULT_LOCK_FACTORY_BEAN)
        public RedisLockFactory redisLockFactory(RedisTemplate redisTemplate, RedisLockHandler redisLockHandler){
            return new RedisLockFactory(redisTemplate,redisLockHandler);
        }
    }

}
