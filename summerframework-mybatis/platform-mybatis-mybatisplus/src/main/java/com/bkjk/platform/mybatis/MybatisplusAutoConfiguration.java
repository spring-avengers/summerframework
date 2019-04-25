package com.bkjk.platform.mybatis;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.bkjk.platform.mybatis.injector.MySqlInjector;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
public class MybatisplusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PaginationInterceptor.class)
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }

    @Bean
    public ISqlInjector sqlInjector() {
        return new MySqlInjector();
    }

    @Value("${mybatis-plus.typeEnumsPackage:}")
    private String typeEnumsPackage;

    @Bean
    @ConditionalOnProperty(value = "platform.mybatis.smart-enum",havingValue = "true",matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CustomizeSqlSessionFactory customizeSqlSessionFactory(SqlSessionFactory sqlSessionFactory, ApplicationContext applicationContext){
        return new CustomizeSqlSessionFactory(typeEnumsPackage,sqlSessionFactory,applicationContext);
    }

}
