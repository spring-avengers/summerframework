package com.bkjk.platform.rabbit.utils;

import static ch.qos.logback.core.spi.FilterReply.ACCEPT;
import static ch.qos.logback.core.spi.FilterReply.DENY;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;

public class LogbackUtil {

    protected static class AppenderBuild {

        public static LevelFilter getLevelFilter(Level level) {
            LevelFilter levelFilter = new LevelFilter();
            levelFilter.setLevel(level);
            levelFilter.setOnMatch(ACCEPT);
            levelFilter.setOnMismatch(DENY);
            return levelFilter;
        }

        public RollingFileAppender getAppender(String name, Level level, String fileName) {
            DateFormat format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.SIMPLIFIED_CHINESE);
            LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();

            RollingFileAppender appender = new RollingFileAppender();

            LevelFilter levelFilter = getLevelFilter(level);
            levelFilter.start();
            appender.addFilter(levelFilter);

            appender.setContext(context);

            appender.setName(name);

            appender.setFile(OptionHelper.substVars("/opt/app/logs/" + fileName + ".json", context));

            appender.setAppend(true);

            appender.setPrudent(false);

            SizeAndTimeBasedRollingPolicy policy = new SizeAndTimeBasedRollingPolicy();

            String fp = OptionHelper.substVars(
                "/opt/app/logs/histroy" + fileName + format.format(new Date()) + "/.%d{yyyy-MM-dd}.%i.json", context);

            policy.setMaxFileSize(FileSize.valueOf("128MB"));

            policy.setFileNamePattern(fp);

            policy.setMaxHistory(7);

            policy.setTotalSizeCap(FileSize.valueOf("32GB"));

            policy.setParent(appender);

            policy.setContext(context);
            policy.start();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();

            encoder.setContext(context);

            encoder.setPattern("%d %p (%file:%line\\)- %m%n");
            encoder.start();

            appender.setRollingPolicy(policy);
            appender.setEncoder(encoder);
            appender.start();
            return appender;
        }
    }

    private static final Map<String, Logger> container = new ConcurrentHashMap<>();

    private static Logger build(String name, Level logLevel, String fileName) {
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(name);
        if (Level.ALL.equals(logLevel)) {
            RollingFileAppender errorAppender = new AppenderBuild().getAppender(name, Level.ERROR, fileName);
            RollingFileAppender infoAppender = new AppenderBuild().getAppender(name, Level.INFO, fileName);
            RollingFileAppender warnAppender = new AppenderBuild().getAppender(name, Level.WARN, fileName);
            RollingFileAppender debugAppender = new AppenderBuild().getAppender(name, Level.DEBUG, fileName);

            logger.setAdditive(false);
            logger.addAppender(errorAppender);
            logger.addAppender(infoAppender);
            logger.addAppender(warnAppender);
            logger.addAppender(debugAppender);

        } else {
            logger = buildInfoByLevel(name, logLevel, fileName);
        }
        return logger;
    }

    private static Logger buildInfoByLevel(String name, Level logLevel, String fileName) {
        RollingFileAppender appender = new AppenderBuild().getAppender(name, logLevel, fileName);
        LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(name);

        logger.setAdditive(false);
        logger.addAppender(appender);
        return logger;
    }

    public static Logger getLogger(String name, Level logLevel, String fileName) {
        Logger logger = container.get(name);
        if (null != logger) {
            return logger;
        }
        synchronized (LogbackUtil.class) {
            logger = container.get(name);
            if (null != logger) {
                return logger;
            }
            logger = build(name, logLevel, fileName);
            container.put(name, logger);
        }
        return logger;
    }

}
