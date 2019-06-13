package com.bkjk.platform.webapi;

import java.lang.reflect.Field;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ReflectionUtils;

import com.bkjk.platform.webapi.swagger.HandlerMethodResolverWrapper;
import com.fasterxml.classmate.TypeResolver;

import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

public class WebApiApplictionInitalizer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().addBeanPostProcessor(new InstantiationAwareBeanPostProcessorAdapter() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HandlerMethodResolver) {
                    HandlerMethodResolver handlerMethodResolver = (HandlerMethodResolver)bean;
                    Field typeResolverField =
                        ReflectionUtils.findField(HandlerMethodResolver.class, "typeResolver", TypeResolver.class);
                    ReflectionUtils.makeAccessible(typeResolverField);
                    TypeResolver typeResolver =
                        (TypeResolver)ReflectionUtils.getField(typeResolverField, handlerMethodResolver);
                    return new HandlerMethodResolverWrapper(typeResolver);
                } else {
                    return bean;
                }

            }
        });

    }

}
