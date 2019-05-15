package com.bkjk.platform.lock.autoconfigure;

import com.bkjk.platform.lock.LockAspect;
import com.bkjk.platform.lock.LockOperation;
import com.bkjk.platform.lock.concurrent.JavaConcurrentLockFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 9:32
 **/
@ConditionalOnProperty(value =  LockConfiguration.PREFIX+".enable",matchIfMissing = true)
@EnableConfigurationProperties(LockConfiguration.class)
@Slf4j
public class LockAutoConfiguration {

    public static final String DEFAULT_LOCK_FACTORY_BEAN="defaultLockFactory";

    @Configuration
    @ConditionalOnMissingBean(name = DEFAULT_LOCK_FACTORY_BEAN)
    public static class JavaConcurrentLockAutoConfiguration{

        @Bean(name = DEFAULT_LOCK_FACTORY_BEAN)
        public JavaConcurrentLockFactory javaConcurrentLockFactory(){
            log.warn("Warning !!! You are using java.concurrent.Lock");
            return new JavaConcurrentLockFactory();
        }
    }

    @Bean
    public LockAspect lockAspect(){
        return new LockAspect();
    }

    @Bean
    public LockOperation lockOperation(){
        return new LockOperation();
    }
}
