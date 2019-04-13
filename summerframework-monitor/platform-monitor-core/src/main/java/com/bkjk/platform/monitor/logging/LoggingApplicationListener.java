package com.bkjk.platform.monitor.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

import com.bkjk.platform.monitor.logging.exception.GlobalUncaughtExceptionHandler;

public class LoggingApplicationListener implements SpringApplicationRunListener {

    public static final Logger logger = LoggerFactory.getLogger(LoggingApplicationListener.class);

    public LoggingApplicationListener(SpringApplication application, String[] args) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        try {
            if (this.isPresent("ch.qos.logback.classic.Logger") && this.isPresent("ch.qos.logback.core.Appender")) {
                new LogBackConfiguration(environment).init();
            }
            if (this.isPresent("org.apache.logging.log4j.Logger")
                && this.isPresent("org.apache.logging.log4j.core.Layout")) {
                new Log4j2Configuration(environment).init();
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        Thread.setDefaultUncaughtExceptionHandler(new GlobalUncaughtExceptionHandler());
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

    private Class<?> forName(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader != null) {
            return classLoader.loadClass(className);
        }
        return Class.forName(className);
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
