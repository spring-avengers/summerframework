package com.bkjk.platform.configcenter;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringUtils;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.foundation.internals.provider.DefaultApplicationProvider;

@Order(value = 1)
public class ApolloSpringApplicationRunListener implements SpringApplicationRunListener {

    private static final String APOLLO_APP_ID_KEY = "app.id";
    private static final String APOLLO_ENV_KEY = "env";
    private static final String APOLLO_META_KEY = "apollo.meta";
    private static final String APOLLO_BOOTSTRAP_ENABLE_KEY = "apollo.bootstrap.enabled";
    private static final String SPRINGBOOT_APPLICATION_NAME = "spring.application.name";
    private static final String SPRINGBOOT_PROFILES_ACTIVE = "spring.profiles.active";
    private static final Properties ENV_PROPERTIES = new Properties();

    public ApolloSpringApplicationRunListener(SpringApplication application, String[] args) {
        try {
            Resource[] resources =
                new PathMatchingResourcePatternResolver().getResources("classpath*:META-INF/apollo.properties");
            for (Resource resource : resources) {
                PropertiesLoaderUtils.fillProperties(ENV_PROPERTIES, resource);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Env apolloEnv = Env.fromString(active);
        System.setProperty(APOLLO_ENV_KEY, apolloEnv.name());
        System.setProperty(APOLLO_META_KEY, ENV_PROPERTIES.getProperty(apolloEnv.name()));
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
