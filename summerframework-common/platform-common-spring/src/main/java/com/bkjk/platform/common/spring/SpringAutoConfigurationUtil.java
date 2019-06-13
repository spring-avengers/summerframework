package com.bkjk.platform.common.spring;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

public class SpringAutoConfigurationUtil {

    private static Logger logger = LoggerFactory.getLogger(SpringAutoConfigurationUtil.class);

    private static final String EXCLUDE_AUTO_CONFIGURATION_START = "spring.autoconfigure.exclude[0]";

    public static final void excludeAutoConfiguration(ConfigurableEnvironment environment,
        SpringApplication application, String autoConfiguration) {

        try {
            MutablePropertySources mutablePropertySources = environment.getPropertySources();
            Iterator<PropertySource<?>> iterator = mutablePropertySources.iterator();
            boolean append = false;
            while (iterator.hasNext()) {
                PropertySource propertySource = iterator.next();
                if (propertySource.containsProperty(EXCLUDE_AUTO_CONFIGURATION_START)) {
                    int i = 1;
                    String key = "spring.autoconfigure.exclude[" + i + "]";
                    while (propertySource.containsProperty(key)) {
                        i++;
                    }
                    if (propertySource.getSource() instanceof Map) {
                        ((Map)propertySource.getSource()).put(key, autoConfiguration);
                        append = true;
                    }
                    break;
                }
            }

            if (!append) {
                Properties propertySource = new Properties();
                propertySource.setProperty(EXCLUDE_AUTO_CONFIGURATION_START, autoConfiguration);
                EnumerablePropertySource enumerablePropertySource =
                    new PropertiesPropertySource("redisSource", propertySource);
                mutablePropertySources.addLast(enumerablePropertySource);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
