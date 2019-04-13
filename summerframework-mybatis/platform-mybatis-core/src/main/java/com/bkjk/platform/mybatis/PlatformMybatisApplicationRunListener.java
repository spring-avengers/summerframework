package com.bkjk.platform.mybatis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class PlatformMybatisApplicationRunListener implements SpringApplicationRunListener {

    private static final String MYBATIS_TYPE_HANDLERS_PACKAGE = "mybatis.type-handlers-package";

    private static final String MYBATIS_TYPE_HANDLERS_PACKAGE_VALUE = "com.bkjk.platform.mybatis.handler";

    // summer框架增加的配置用 platform. 开头
    public static final String MYBATIS_PROPERTIES_PREFIX = "platform.";
    public static final String MYBATIS_ENCRYPT_PASSWORD = MYBATIS_PROPERTIES_PREFIX + "mybatis.encrypt.password";
    public static final String MYBATIS_ENCRYPT_SALT = MYBATIS_PROPERTIES_PREFIX + "mybatis.encrypt.salt";

    public static final String SHA1_COLUMN_HANDLER_SALT = MYBATIS_PROPERTIES_PREFIX + "mybatis.sha1-column.salt";

    public PlatformMybatisApplicationRunListener(SpringApplication application, String[] args) {
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

        if (environment.containsProperty(MYBATIS_ENCRYPT_PASSWORD)) {
            System.setProperty(MYBATIS_ENCRYPT_PASSWORD, environment.getProperty(MYBATIS_ENCRYPT_PASSWORD));
        }

        if (environment.containsProperty(MYBATIS_ENCRYPT_SALT)) {
            System.setProperty(MYBATIS_ENCRYPT_SALT, environment.getProperty(MYBATIS_ENCRYPT_SALT));
        }

        if (environment.containsProperty(SHA1_COLUMN_HANDLER_SALT)) {
            System.setProperty(SHA1_COLUMN_HANDLER_SALT, environment.getProperty(SHA1_COLUMN_HANDLER_SALT));
        }
        if (environment.containsProperty(MYBATIS_TYPE_HANDLERS_PACKAGE)) {
            props.put(MYBATIS_TYPE_HANDLERS_PACKAGE,
                environment.getProperty(MYBATIS_TYPE_HANDLERS_PACKAGE) + "," + MYBATIS_TYPE_HANDLERS_PACKAGE_VALUE);
        } else {
            props.put(MYBATIS_TYPE_HANDLERS_PACKAGE, MYBATIS_TYPE_HANDLERS_PACKAGE_VALUE);
        }
        environment.getPropertySources().addFirst(new PropertiesPropertySource("summerframeworkMybatis", props));
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
