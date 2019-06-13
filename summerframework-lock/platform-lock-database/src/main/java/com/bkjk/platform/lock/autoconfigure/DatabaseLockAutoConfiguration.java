package com.bkjk.platform.lock.autoconfigure;

import com.bkjk.platform.lock.database.DatabaseLockFactory;
import com.bkjk.platform.lock.database.DatabaseLockHandler;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/16 19:34
 **/
@ConditionalOnProperty(value =  LockConfiguration.PREFIX+".enable",matchIfMissing = true)
@EnableConfigurationProperties(LockConfiguration.class)
@AutoConfigureBefore(LockAutoConfiguration.class)
public class DatabaseLockAutoConfiguration {

    public static final String DATABASE_LOCK_FACTORY_BEAN="databaseLockFactory";

    @Configuration
    @ConditionalOnMissingBean(name = DATABASE_LOCK_FACTORY_BEAN)
    @ConditionalOnBean(DataSource.class)
    public static class LockFactoryAutoConfiguration{

        @Bean
        public DatabaseLockHandler databaseLockHandler(){
            return new DatabaseLockHandler();
        }

        @Bean(name = DATABASE_LOCK_FACTORY_BEAN)
        public DatabaseLockFactory databaseLockFactory(DataSource dataSource, DatabaseLockHandler databaseLockHandler){
            return new DatabaseLockFactory(dataSource,databaseLockHandler);
        }
    }

}
