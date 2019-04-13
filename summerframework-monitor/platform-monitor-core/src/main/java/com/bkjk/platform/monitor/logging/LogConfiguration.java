package com.bkjk.platform.monitor.logging;

import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.LOG_KAFKA_BOOTSTRAPSERVERS;
import static com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener.LOG_KAFKA_TOPIC;

import org.springframework.core.env.Environment;

import com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener;

public abstract class LogConfiguration {

    protected final Environment env;

    private static final String LOG_PATTERN_KEY = "platform.monitor.log.pattern";

    protected static final String LOGBACK_PATTERN =
        "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%-5level] [%logger{40}] [%tid] - %msg%n";

    protected static final String LOG4J2_PATTERN =
        "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%-5level] [%logger{40}] [%traceId] - %msg%n";

    protected String getBootstrapservers() {
        return MonitorConfigSpringApplicationRunListener.getConfig(LOG_KAFKA_BOOTSTRAPSERVERS, null);
    }

    protected String getKafkaTopic() {
        return MonitorConfigSpringApplicationRunListener.getConfig(LOG_KAFKA_TOPIC, "bizlog");
    }

    protected String getLogBackPattern() {
        return env.getProperty(LOG_PATTERN_KEY, LOGBACK_PATTERN);
    }

    protected String getLog4jPattern() {
        return env.getProperty(LOG_PATTERN_KEY, LOG4J2_PATTERN);
    }

    public LogConfiguration(Environment env) {
        this.env = env;
    }

    public abstract void init();

}
