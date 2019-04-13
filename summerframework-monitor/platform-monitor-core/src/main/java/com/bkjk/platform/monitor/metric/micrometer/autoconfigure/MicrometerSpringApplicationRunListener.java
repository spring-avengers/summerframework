package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_BATCHSIZE;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_DB;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_ENABLED;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_PASSWORD;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_READTIMEOUT;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_RETENTION_DURATION;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_RETENTION_POLICY;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_STEP;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_URI;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.METRICS_INFLUX_USERNAME;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener;

/**
 * 该类必须在MonitorConfigSpringApplicationRunListener之后，把配置变量读取到后再加载
 */
@Order(value = 3)
public class MicrometerSpringApplicationRunListener implements SpringApplicationRunListener {

    public static final Logger logger = LoggerFactory.getLogger(MicrometerSpringApplicationRunListener.class);

    public MicrometerSpringApplicationRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

    }

    private Properties createProperties(ConfigurableEnvironment environment) {
        Properties props = new Properties();
        props.setProperty(METRICS_INFLUX_ENABLED.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_ENABLED, "true"));
        props.setProperty(METRICS_INFLUX_URI.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_URI, null));
        props.setProperty(METRICS_INFLUX_DB.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_DB, "micrometerDb"));
        props.setProperty(METRICS_INFLUX_STEP.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_STEP, "5S"));
        props.setProperty(METRICS_INFLUX_READTIMEOUT.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_READTIMEOUT, "30S"));
        props.setProperty(METRICS_INFLUX_BATCHSIZE.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_BATCHSIZE, "2000"));
        props.setProperty(METRICS_INFLUX_USERNAME.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_USERNAME, ""));
        props.setProperty(METRICS_INFLUX_PASSWORD.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_PASSWORD, ""));
        props.setProperty(METRICS_INFLUX_RETENTION_DURATION.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_RETENTION_DURATION, "7d"));
        props.setProperty(METRICS_INFLUX_RETENTION_POLICY.toLowerCase(),
            MonitorConfigSpringApplicationRunListener.getConfig(METRICS_INFLUX_RETENTION_POLICY, ""));
        return props;
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        new TransactionManagerCustomizer().apply(environment);
        if (environment.getProperty(METRICS_INFLUX_ENABLED) == null) {
            logger.info("{} not set. Default enable", METRICS_INFLUX_ENABLED);
            Properties props = createProperties(environment);
            logger.info("summerframeworkMicrometer = {}", props);
            environment.getPropertySources().addLast(new PropertiesPropertySource("summerframeworkMicrometer", props));
        }
    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {

    }

    @Override
    public void running(ConfigurableApplicationContext context) {
        HealthMetricsConfiguration.running();
    }

    @Override
    public void started(ConfigurableApplicationContext context) {

    }

    @Override
    public void starting() {

    }

}
