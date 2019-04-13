package com.bkjk.platform.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 该类必须在ConfigCenter的ApolloSpringApplicationRunListener之后
 */
@Order(value = 2)
public class MonitorConfigSpringApplicationRunListener implements SpringApplicationRunListener {

    private static Logger LOGGER = LoggerFactory.getLogger(MonitorConfigSpringApplicationRunListener.class);

    public MonitorConfigSpringApplicationRunListener(SpringApplication application, String[] args) {
    }

    public static final String METRICS_INFLUX_ENABLED = "management.metrics.export.influx.enabled";
    public static final String METRICS_INFLUX_DB = "management.metrics.export.influx.db";
    public static final String METRICS_INFLUX_URI = "management.metrics.export.influx.uri";
    public static final String METRICS_INFLUX_STEP = "management.metrics.export.influx.step";
    public static final String METRICS_INFLUX_RETENTION_DURATION =
        "management.metrics.export.influx.retention-duration";
    public static final String METRICS_INFLUX_RETENTION_POLICY = "management.metrics.export.influx.retention-policy";
    public static final String METRICS_INFLUX_READTIMEOUT = "management.metrics.export.influx.read-timeout";
    public static final String METRICS_INFLUX_BATCHSIZE = "management.metrics.export.influx.batch-size";
    public static final String METRICS_INFLUX_USERNAME = "management.metrics.export.influx.user-name";
    public static final String METRICS_INFLUX_PASSWORD = "management.metrics.export.influx.password";
    public static final String METRICS_INFLUX_TIMER_PERCENTILES = "management.metric.export.timer-percentiles";
    public static final String METRICS_INFLUX_CLEANER_TRIGGER = "management.metric.export.cleaner-trigger";
    public static final String METRICS_INFLUX_TAGS_IGNORE = "management.metric.export.tags-ignore";
    public static final String LOG_KAFKA_TOPIC = "managent.logs.kafka.topic";
    public static final String LOG_KAFKA_BOOTSTRAPSERVERS = "managent.logs.kafka.bootstrapservers";

    private static ConfigurableEnvironment ENVIRONMENT;

    // 这里转大写，是因为Apollo配置中心是大写
    public static final String getConfig(String key, String defaultValue) {
        String value = ENVIRONMENT.getProperty(key.toUpperCase(), defaultValue);
        if (value == null) {
            value = ENVIRONMENT.getProperty(key, defaultValue);
        }
        if (value == null) {
            LOGGER.warn("not found " + key + " in Environment, please config " + key + "'s value");
        }
        return value;
    }

    @Override
    public void starting() {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        MonitorConfigSpringApplicationRunListener.ENVIRONMENT = environment;
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void started(ConfigurableApplicationContext context) {

    }

    @Override
    public void running(ConfigurableApplicationContext context) {

    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

}
