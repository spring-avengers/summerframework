package com.bkjk.platform.monitor.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AsyncAppenderBase;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.AppenderAttachable;
import com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener;
import com.bkjk.platform.monitor.logging.appender.logback.AdvancedKafkaAppender;
import com.bkjk.platform.monitor.logging.appender.logback.layout.CustomJsonLayout;
import org.apache.skywalking.apm.toolkit.log.logback.v1.x.TraceIdPatternLogbackLayout;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

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
            AppenderAttachable appenderAttachable = logger;
            setLayout(loggerContext,appenderAttachable.iteratorForAppenders());
        }
    }

    private void setLayout(LoggerContext loggerContext,Iterator<Appender<?>> iterator){
        while (iterator.hasNext()) {
            Appender appender = iterator.next();
            if (appender instanceof OutputStreamAppender) {
                setLayout(loggerContext,(OutputStreamAppender<?>)appender);
            }else if(appender instanceof AsyncAppenderBase){
                AsyncAppenderBase asyncAppenderBase= (AsyncAppenderBase) appender;
                setLayout(loggerContext,asyncAppenderBase.iteratorForAppenders());
            }
        }
    }

    private void setLayout(LoggerContext loggerContext,OutputStreamAppender outputStreamAppender){
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
