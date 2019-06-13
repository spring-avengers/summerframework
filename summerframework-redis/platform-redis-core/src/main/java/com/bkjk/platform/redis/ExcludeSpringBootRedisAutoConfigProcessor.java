package com.bkjk.platform.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

import com.bkjk.platform.common.spring.SpringAutoConfigurationUtil;

public class ExcludeSpringBootRedisAutoConfigProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String REDIS_AUTOCONFIGURATION =
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration";

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean isPresent = isPresent("org.redisson.api.RedissonClient");
        if (isPresent)
            SpringAutoConfigurationUtil.excludeAutoConfiguration(environment, application, REDIS_AUTOCONFIGURATION);
    }

    private boolean isPresent(String className) {
        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
        try {
            forName(className, classLoader);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private Class<?> forName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader != null) {
            return classLoader.loadClass(className);
        }
        return Class.forName(className);
    }
}
