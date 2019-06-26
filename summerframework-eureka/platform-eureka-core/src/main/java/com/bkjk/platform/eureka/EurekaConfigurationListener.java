package com.bkjk.platform.eureka;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtils.HostInfo;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

public class EurekaConfigurationListener implements SpringApplicationRunListener {
    private static final String PREFER_IP_ADDRESS = "eureka.instance.prefer-ip-address";
    private static final String PREFER_INSTANCE_ID_ADDRESS = "eureka.instance.instance-id";
    private static final String SERVERPORT = "server.port";

    private static final Logger logger = LoggerFactory.getLogger(EurekaConfigurationListener.class);

    private static final ConcurrentHashMap<String, Object> defaultProperties = new ConcurrentHashMap<>();

    public EurekaConfigurationListener(SpringApplication application, String[] args) {}

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    private HostInfo getFirstNonLoopbackHostInfo(ConfigurableEnvironment environment) {
        InetUtilsProperties target = new InetUtilsProperties();
        ConfigurationPropertySources.attach(environment);
        Binder.get(environment).bind(InetUtilsProperties.PREFIX, Bindable.ofInstance(target));
        try (InetUtils utils = new InetUtils(target)) {
            return utils.findFirstNonLoopbackHostInfo();
        }
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        Properties props = new Properties();
        props.put(PREFER_IP_ADDRESS, true);
        // 这里提前获取instanceId了，比HostInfoEnvironmentPostProcessor要早
        props.put(PREFER_INSTANCE_ID_ADDRESS, getFirstNonLoopbackHostInfo(environment).getIpAddress() + ":"
            + environment.getProperty(SERVERPORT, "8080"));
        logger.warn("Set {} = true", PREFER_IP_ADDRESS);
        environment.getPropertySources().addFirst(new PropertiesPropertySource("summerframeworkEurekaConfig", props));

        defaultProperties.put("ribbon.MaxAutoRetries", 0);
        defaultProperties.put("ribbon.MaxAutoRetriesNextServer", 0);
        defaultProperties.put("eureka.client.healthcheck.enabled", true);
        defaultProperties.put("eureka.client.instanceInfoReplicationIntervalSeconds", 10);
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
