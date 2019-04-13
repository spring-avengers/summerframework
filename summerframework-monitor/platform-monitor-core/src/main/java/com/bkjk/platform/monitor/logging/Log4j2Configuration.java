package com.bkjk.platform.monitor.logging;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout.Serializer;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.bkjk.platform.monitor.logging.appender.log4j.AdvancedKafkaAppender;
import com.bkjk.platform.monitor.logging.appender.log4j.layout.CustomJsonLayout;

public class Log4j2Configuration extends LogConfiguration {

    public Log4j2Configuration(Environment env) {
        super(env);
    }

    private static final Logger logger = LoggerFactory.getLogger(Log4j2Configuration.class);

    private void createBizLogger() {
        String appenderName = "AdvancedKafkaAppender";
        LoggerContext loggerContext = (LoggerContext)LogManager.getContext(false);
        Configuration configuration = loggerContext.getConfiguration();
        AdvancedKafkaAppender kafkaAppender =
            AdvancedKafkaAppender.createAppender(CustomJsonLayout.createDefaultLayout(), null, configuration,
                appenderName, getKafkaTopic(), getBootstrapservers());
        kafkaAppender.start();
        AppenderRef ref = AppenderRef.createAppenderRef(appenderName, null, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig =
            LoggerConfig.createLogger(false, Level.INFO, "BizLogger", null, refs, null, configuration, null);
        loggerConfig.addAppender(kafkaAppender, null, null);
        configuration.addLogger("BizLogger", loggerConfig);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void init() {
        try {
            LoggerContext loggerContext = (LoggerContext)LogManager.getContext(false);
            if (loggerContext == null)
                return;
            org.apache.logging.log4j.core.Logger logger =
                loggerContext.getLogger("org.apache.kafka.clients.producer.ProducerConfig");
            if (logger != null) {
                logger.setLevel(org.apache.logging.log4j.Level.ERROR);
            }
            createBizLogger();
            Configuration configuration = loggerContext.getConfiguration();
            configuration.getPluginPackages().add("org.apache.skywalking.apm.toolkit.log.log4j.v2.x");
            Map<String, Appender> appenders = configuration.getAppenders();
            for (Appender appender : appenders.values()) {
                Layout<? extends Serializable> layout = appender.getLayout();
                if (layout instanceof PatternLayout) {
                    PatternLayout patternLayOut = (PatternLayout)layout;
                    Serializer serializer = PatternLayout.createSerializer(configuration, null, getLog4jPattern(),
                        getLog4jPattern(), null, true, true);
                    Field field = patternLayOut.getClass().getDeclaredField("eventSerializer");
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    field.set(patternLayOut, serializer);
                }
            }
            loggerContext.updateLoggers();
        } catch (Throwable e) {
            logger.warn(e.getMessage());
        }
    }

}
