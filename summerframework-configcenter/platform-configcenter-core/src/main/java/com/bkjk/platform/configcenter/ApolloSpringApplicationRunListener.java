package com.bkjk.platform.configcenter;

import java.util.Properties;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.util.StringUtils;

import com.bkjk.platform.configcenter.helper.ApolloEnv;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.foundation.internals.provider.DefaultApplicationProvider;

@Order(value = 1)
public class ApolloSpringApplicationRunListener implements SpringApplicationRunListener {

    private static final String APOLLO_APP_ID_KEY = "app.id";
    private static final String APOLLO_ENV_KEY = "env";
    private static final String APOLLO_BOOTSTRAP_ENABLE_KEY = "apollo.bootstrap.enabled";
    private static final String SPRINGBOOT_APPLICATION_NAME = "spring.application.name";
    private static final String SPRINGBOOT_PROFILES_ACTIVE = "spring.profiles.active";
    private static final String CONFIGCENTER_INFRA_NAMESPACE = "BKJK.INFRA-MONITOR";

    public ApolloSpringApplicationRunListener(SpringApplication application, String[] args) {

    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment env) {
        Properties props = new Properties();
        props.put(APOLLO_BOOTSTRAP_ENABLE_KEY, true);
        System.setProperty("spring.banner.location", "classpath:META-INF/banner.txt");
        env.getPropertySources().addFirst(new PropertiesPropertySource("apolloConfig", props));
        // 初始化环境
        this.initEnv(env);
        // 初始化appId
        this.initAppId(env);
        // 初始化架构提供的默认配置
        this.initInfraConfig(env);
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

    private void initAppId(ConfigurableEnvironment env) {
        String applicationName = env.getProperty(SPRINGBOOT_APPLICATION_NAME);
        String apolloAppId = env.getProperty(APOLLO_APP_ID_KEY);
        if (StringUtils.isEmpty(apolloAppId)) {
            if (!StringUtils.isEmpty(applicationName)) {
                System.setProperty(APOLLO_APP_ID_KEY, applicationName);
            } else {
                throw new IllegalArgumentException(
                    "Config center must config app.id in " + DefaultApplicationProvider.APP_PROPERTIES_CLASSPATH);
            }
        } else {
            System.setProperty(APOLLO_APP_ID_KEY, apolloAppId);
        }
    }

    private void initEnv(ConfigurableEnvironment env) {
        String active = env.getProperty(SPRINGBOOT_PROFILES_ACTIVE);
        ApolloEnv apolloEnv = ApolloEnv.fromTypeName(active);
        System.setProperty(APOLLO_ENV_KEY, apolloEnv.name());
        switch (apolloEnv) {
            case DEV:
                System.setProperty(ApolloEnv.DEV.getEnvMetaKey(), ApolloEnv.DEV.getEnvMetaValue());
                break;
            case TEST:
                System.setProperty(ApolloEnv.TEST.getEnvMetaKey(), ApolloEnv.TEST.getEnvMetaValue());
                break;
            case STAGE:
                System.setProperty(ApolloEnv.STAGE.getEnvMetaKey(), ApolloEnv.STAGE.getEnvMetaValue());
                break;
            case PROD:
                System.setProperty(ApolloEnv.PROD.getEnvMetaKey(), ApolloEnv.PROD.getEnvMetaValue());
                break;
            default:
                System.setProperty(ApolloEnv.DEV.getEnvMetaKey(), ApolloEnv.DEV.getEnvMetaValue());
                break;
        }
    }

    private void initInfraConfig(ConfigurableEnvironment env) {
        com.ctrip.framework.apollo.Config apolloConfig = ConfigService.getConfig(CONFIGCENTER_INFRA_NAMESPACE);
        Set<String> propertyNames = apolloConfig.getPropertyNames();
        if (propertyNames != null && propertyNames.size() > 0) {
            Properties propertes = new Properties();
            for (String propertyName : propertyNames) {
                propertes.setProperty(propertyName, apolloConfig.getProperty(propertyName, null));
            }
            EnumerablePropertySource enumerablePropertySource =
                new PropertiesPropertySource(CONFIGCENTER_INFRA_NAMESPACE, propertes);
            env.getPropertySources().addLast(enumerablePropertySource);
        }
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
