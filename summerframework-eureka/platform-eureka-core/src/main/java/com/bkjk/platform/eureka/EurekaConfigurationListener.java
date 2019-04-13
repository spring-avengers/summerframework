package com.bkjk.platform.eureka;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

public class EurekaConfigurationListener implements SpringApplicationRunListener {
    private static final Object PREFER_IP_ADDRESS = "eureka.instance.prefer-ip-address";

    private static final Logger logger = LoggerFactory.getLogger(EurekaConfigurationListener.class);

    private static final ConcurrentHashMap<String, Object> defaultProperties = new ConcurrentHashMap<>();

    public EurekaConfigurationListener(SpringApplication application, String[] args) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        Properties props = new Properties();
        props.put(PREFER_IP_ADDRESS, true);
        logger.warn("Set {} = true", PREFER_IP_ADDRESS);
        environment.getPropertySources().addFirst(new PropertiesPropertySource("summerframeworkEurekaConfig", props));

        defaultProperties.put("ribbon.MaxAutoRetries", 0);
        defaultProperties.put("ribbon.MaxAutoRetriesNextServer", 0);
        Properties lastDefaultProps = new Properties();
        defaultProperties.forEach((k, v) -> {
            if (!environment.containsProperty(k)) {
                lastDefaultProps.put(k, v);
            }
        });
        if (!lastDefaultProps.isEmpty()) {
            logger.warn("Some dangerous properties was NOT set by user. Give it default value");
            logger.warn(lastDefaultProps.toString());
            environment.getPropertySources()
                .addLast(new PropertiesPropertySource("summerframeworkEurekaConfigLast", props));
        }

    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

    @Override
    public void running(ConfigurableApplicationContext context) {

    }

    @Override
    public void started(ConfigurableApplicationContext context) {

    }

    @Override
    public void starting() {

    }

}
