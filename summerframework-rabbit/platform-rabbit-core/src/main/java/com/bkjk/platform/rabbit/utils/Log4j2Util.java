package com.bkjk.platform.rabbit.utils;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.appender.rolling.action.DeleteAction;
import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.appender.rolling.action.IfFileName;
import org.apache.logging.log4j.core.appender.rolling.action.IfLastModified;
import org.apache.logging.log4j.core.appender.rolling.action.PathCondition;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class Log4j2Util {

    private static final String datalogDir = "/opt/app/logs/";

    private static final LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
    private static final Configuration config = ctx.getConfiguration();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void createAppenderAndStart(String loggerName, String fileName, String filePattern) {

        Layout layout = PatternLayout.newBuilder().withConfiguration(config)
            .withPattern("[%d{HH:mm:ss:SSS}] [%p] - %l - %m%n").build();
        TimeBasedTriggeringPolicy tbtp = TimeBasedTriggeringPolicy.createPolicy(null, null);
        TriggeringPolicy tp = SizeBasedTriggeringPolicy.createPolicy("10M");
        CompositeTriggeringPolicy policyComposite = CompositeTriggeringPolicy.createPolicy(tbtp, tp);

        String loggerDir = datalogDir + File.separator;

        String loggerPathPrefix = loggerDir + File.separator;
        RollingFileAppender.Builder builder = RollingFileAppender.newBuilder().withFilePattern(filePattern)
            .withStrategy(null).withPolicy(policyComposite).withConfiguration(config);
        RollingFileAppender appender = builder.build();
        appender.start();
        config.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef(loggerName, Level.INFO, null);
        AppenderRef[] refs = new AppenderRef[] {ref};
        LoggerConfig loggerConfig =
            LoggerConfig.createLogger(false, Level.ALL, loggerName, "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger(loggerName, loggerConfig);
        ctx.updateLoggers();
    }

    private static DefaultRolloverStrategy createStrategyByAction(String loggerName, String loggerDir) {

        IfFileName ifFileName = IfFileName.createNameCondition(null, loggerName + "\\.\\d{4}-\\d{2}-\\d{2}.*");
        IfLastModified ifLastModified = IfLastModified.createAgeCondition(Duration.parse("1d"));
        DeleteAction deleteAction = DeleteAction.createDeleteAction(loggerDir, false, 1, false, null,
            new PathCondition[] {ifLastModified, ifFileName}, null, config);
        Action[] actions = new Action[] {deleteAction};

        return DefaultRolloverStrategy.createStrategy("7", "1", null, null, actions, false, config);
    }

    public static Logger getLogger(String loggerName, String fileName, String filePattern) {
        synchronized (config) {
            if (!config.getLoggers().containsKey(loggerName)) {
                createAppenderAndStart(loggerName, fileName, filePattern);
            }
        }
        return LogManager.getLogger(loggerName);
    }

    public static void removeLogger(String loggerName) {
        synchronized (config) {
            config.getAppender(loggerName).stop();
            config.getLoggerConfig(loggerName).removeAppender(loggerName);
            config.removeLogger(loggerName);
            ctx.updateLoggers();
        }
    }

    private Log4j2Util() {
    }
}
