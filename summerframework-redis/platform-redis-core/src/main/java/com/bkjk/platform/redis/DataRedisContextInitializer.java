package com.bkjk.platform.redis;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.ReflectionUtils;

import com.bkjk.platform.redis.data.DataRedisProxyHandler;

public class DataRedisContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        applicationContext.getBeanFactory().addBeanPostProcessor(new InstantiationAwareBeanPostProcessorAdapter() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof RedisTemplate) {
                    RedisTemplate redisTemplate = (RedisTemplate)bean;
                    // do cache
                    redisTemplate.opsForValue();
                    redisTemplate.opsForList();
                    redisTemplate.opsForSet();
                    redisTemplate.opsForZSet();
                    redisTemplate.opsForGeo();
                    redisTemplate.opsForHash();
                    redisTemplate.opsForHyperLogLog();
                    createProxyHandlers(redisTemplate);
                }
                return bean;
            }
        });
    }

    private void createProxyHandlers(RedisTemplate redisTemplate) {
        createProxyHandler(redisTemplate, ValueOperations.class, "valueOps");
        createProxyHandler(redisTemplate, ListOperations.class, "listOps");
        createProxyHandler(redisTemplate, SetOperations.class, "setOps");
        createProxyHandler(redisTemplate, ZSetOperations.class, "zSetOps");
        createProxyHandler(redisTemplate, GeoOperations.class, "geoOps");
        createProxyHandler(redisTemplate, HyperLogLogOperations.class, "hllOps");
    }

    private void createProxyHandler(RedisTemplate redisTemplate, Class clazz, String name) {
        try {
            Field field = ReflectionUtils.findField(RedisTemplate.class, name, clazz);
            ReflectionUtils.makeAccessible(field);
            Object object = ReflectionUtils.getField(field, redisTemplate);
            DataRedisProxyHandler handler = new DataRedisProxyHandler(object);
            Object proxy = Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] {clazz}, handler);
            field.set(redisTemplate, proxy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
