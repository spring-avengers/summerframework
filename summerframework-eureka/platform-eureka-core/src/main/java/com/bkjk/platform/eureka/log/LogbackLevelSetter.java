package com.bkjk.platform.eureka.log;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class LogbackLevelSetter implements LogLevelSetter {
    @Override
    public void setLoggerLevel(String name, String level) {
        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        List<Logger> loggerList = loggerContext.getLoggerList();
        for (ch.qos.logback.classic.Logger logger : loggerList) {
            if (name.equals(logger.getName())) {
                logger.setLevel(Level.valueOf(level));
            }
        }
    }
}
