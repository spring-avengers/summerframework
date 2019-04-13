package com.bkjk.platform.eureka.log;

import java.util.Enumeration;

import org.apache.log4j.LogManager;

public class Log4jLevelSetter implements LogLevelSetter {
    @Override
    public void setLoggerLevel(String name, String level) {
        Enumeration enumeration = LogManager.getCurrentLoggers();
        while (enumeration.hasMoreElements()) {
            org.apache.log4j.Logger logger = (org.apache.log4j.Logger)enumeration.nextElement();
            if (name.equals(logger.getName())) {
                logger.setLevel(org.apache.log4j.Level.toLevel(level));
            }
        }
    }
}
