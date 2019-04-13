package com.bkjk.platform.eureka.log;

import java.util.Collection;

import org.apache.logging.log4j.core.Logger;

public class Log4j2LevelSetter implements LogLevelSetter {
    @Override
    public void setLoggerLevel(String name, String level) {
        Collection<Logger> notCurrentLoggerCollection =
            org.apache.logging.log4j.core.LoggerContext.getContext(false).getLoggers();
        Collection<org.apache.logging.log4j.core.Logger> currentLoggerCollection =
            org.apache.logging.log4j.core.LoggerContext.getContext().getLoggers();
        Collection<org.apache.logging.log4j.core.Logger> loggerCollection = notCurrentLoggerCollection;
        loggerCollection.addAll(currentLoggerCollection);
        for (org.apache.logging.log4j.core.Logger logger : loggerCollection) {
            if (name.equals(logger.getName())) {
                logger.setLevel(org.apache.logging.log4j.Level.toLevel(level));
            }
        }
    }
}
