package com.bkjk.platform.mybatis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class MybatisplusApplicationRunListener implements SpringApplicationRunListener {

    private static final String MYBATIS_TYPE_HANDLERS_PACKAGE = "mybatis-plus.type-handlers-package";

    private static final String MYBATIS_TYPE_HANDLERS_PACKAGE_VALUE = "com.bkjk.platform.mybatis.handler";

    private static final String ID_TYPE = "mybatis-plus.global-config.db-config.id-type";
    private static final String ID_TYPE_AUTO = "auto";

    public MybatisplusApplicationRunListener(SpringApplication application, String[] args) {
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
        if (!environment.containsProperty(ID_TYPE)) {

            props.put(ID_TYPE, ID_TYPE_AUTO);
        }

        if (environment.containsProperty(MYBATIS_TYPE_HANDLERS_PACKAGE)) {
            props.put(MYBATIS_TYPE_HANDLERS_PACKAGE,
                environment.getProperty(MYBATIS_TYPE_HANDLERS_PACKAGE) + "," + MYBATIS_TYPE_HANDLERS_PACKAGE_VALUE);
        } else {
            props.put(MYBATIS_TYPE_HANDLERS_PACKAGE, MYBATIS_TYPE_HANDLERS_PACKAGE_VALUE);
        }
        environment.getPropertySources().addFirst(new PropertiesPropertySource("summerframeworkMybatisplus", props));
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
