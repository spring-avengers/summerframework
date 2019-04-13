package com.bkjk.platform.monitor.metric.micrometer.binder.db;

import com.p6spy.engine.spy.P6DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

public class P6DataSourceBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof P6DataSource)) {
            P6DataSource dataSource = new P6DataSource((DataSource)bean);
            dataSource.setRealDataSource(beanName);
            return dataSource;
        } else {
            return bean;
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
