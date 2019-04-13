package com.bkjk.platform.monitor.logging;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener;
import com.bkjk.platform.monitor.logging.appender.logback.AdvancedKafkaAppender;
import com.bkjk.platform.monitor.logging.appender.logback.layout.CustomJsonLayout;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.AppenderAttachable;

public class LogBackConfiguration extends LogConfiguration {

    public LogBackConfiguration(Environment env) {
        super(env);
    }

    private void createBizLogger() {
        if (env.containsProperty(MonitorConfigSpringApplicationRunListener.LOG_KAFKA_BOOTSTRAPSERVERS)) {
            LoggerContext content = (LoggerContext)LoggerFactory.getILoggerFactory();
            AdvancedKafkaAppender kafkaAppender = new AdvancedKafkaAppender();
            kafkaAppender.setLayout(new CustomJsonLayout());
            kafkaAppender.setTopic(getKafkaTopic());
            kafkaAppender.setBootstrapServers(getBootstrapservers());
            kafkaAppender.setContext(content);
            kafkaAppender.start();
            Logger logger = (Logger)LoggerFactory.getLogger("BizLogger");
            logger.addAppender(kafkaAppender);
            logger.setLevel(Level.INFO);
            logger.setAdditive(false);
        }
    }

    @Override
    public void init() {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        if (loggerContext == null)
            return;
        List<Logger> loggerList = loggerContext.getLoggerList();
        ch.qos.logback.classic.Logger loggerKafka =
            loggerContext.getLogger("org.apache.kafka.clients.producer.ProducerConfig");
        if (loggerKafka != null) {
            loggerKafka.setLevel(ch.qos.logback.classic.Level.ERROR);
        }
        createBizLogger();
        for (Logger logger : loggerList) {
            AppenderAttachable<ILoggingEvent> appenderAttachable = logger;
            Iterator<Appender<ILoggingEvent>> iterator = appenderAttachable.iteratorForAppenders();
            while (iterator.hasNext()) {
                Appender<ILoggingEvent> appender = iterator.next();
                if (appender instanceof OutputStreamAppender) {
                    OutputStreamAppender<?> outputStreamAppender = (OutputStreamAppender<?>)appender;
                    Encoder<?> encoder = outputStreamAppender.getEncoder();
                    if (encoder instanceof LayoutWrappingEncoder) {
                        TraceIdPatternLogbackLayout traceIdLayOut = new TraceIdPatternLogbackLayout();
                        traceIdLayOut.setContext(loggerContext);
                        traceIdLayOut.setPattern(getLogBackPattern());
                        traceIdLayOut.start();
                        Field field = ReflectionUtils.findField(encoder.getClass(), "layout");
                        field.setAccessible(true);
                        ReflectionUtils.setField(field, encoder, traceIdLayOut);
                    }
                }
            }
        }
    }

}
