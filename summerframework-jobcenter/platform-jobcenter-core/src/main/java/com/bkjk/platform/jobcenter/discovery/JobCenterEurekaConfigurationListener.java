package com.bkjk.platform.jobcenter.discovery;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

public class JobCenterEurekaConfigurationListener implements SpringApplicationRunListener {
    private static final String defaultZoneKey = "eureka.client.serviceUrl.defaultZone";

    private static final String defaultZone = "http://soa-eureka/eureka/";

    public JobCenterEurekaConfigurationListener(SpringApplication application, String[] args) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        if (!environment.containsProperty(defaultZoneKey)) {
            Properties props = new Properties();
            props.put(defaultZoneKey, defaultZone);
            environment.getPropertySources().addFirst(new PropertiesPropertySource("defaultJobCentoerConfig", props));
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
