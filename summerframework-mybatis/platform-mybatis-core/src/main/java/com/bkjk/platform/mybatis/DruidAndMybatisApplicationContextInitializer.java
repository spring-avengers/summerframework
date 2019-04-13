
package com.bkjk.platform.mybatis;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

public class DruidAndMybatisApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            ClassUtils.forName("com.alibaba.druid.pool.DruidDataSource",this.getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return;
        }
        applicationContext.getBeanFactory().addBeanPostProcessor(new InstantiationAwareBeanPostProcessorAdapter() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSourceProperties) {
                    DataSourceProperties dataSourceProperties = (DataSourceProperties)bean;
                    DruidAndMybatisApplicationContextInitializer.this.rewirteDataSourceProperties(dataSourceProperties);
                } else if (bean instanceof MybatisProperties) {
                    MybatisProperties mybatisProperties = (MybatisProperties)bean;
                    DruidAndMybatisApplicationContextInitializer.this.rewriteMybatisProperties(mybatisProperties);
                }
                return bean;
            }
        });

    }

    private void rewirteDataSourceProperties(DataSourceProperties dataSourceProperties) {
        if (dataSourceProperties.getType() == null) {
            dataSourceProperties.setType(com.alibaba.druid.pool.DruidDataSource.class);
        }
        if (dataSourceProperties.getPlatform().equals("all")) {
            dataSourceProperties.setPlatform("mysql");
        }
    }

    private void rewriteMybatisProperties(MybatisProperties mybatisProperties) {
        if (mybatisProperties.getConfiguration() == null) {
            Configuration configuration = new Configuration();
            configuration.setUseGeneratedKeys(true);
            configuration.setMapUnderscoreToCamelCase(true);
            mybatisProperties.setConfiguration(configuration);
        } else {
            Configuration configuration = mybatisProperties.getConfiguration();
            if (configuration.isUseGeneratedKeys()) {
                configuration.setUseGeneratedKeys(true);
            }
            if (configuration.isMapUnderscoreToCamelCase()) {
                configuration.setMapUnderscoreToCamelCase(true);
            }
            mybatisProperties.setConfiguration(configuration);
        }
    }

}
