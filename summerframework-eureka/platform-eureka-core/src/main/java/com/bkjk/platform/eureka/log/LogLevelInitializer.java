package com.bkjk.platform.eureka.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

public class LogLevelInitializer {
    private static Logger logger = LoggerFactory.getLogger(LogLevelInitializer.class);
    @Autowired
    private List<LogLevelSetter> logLevelSetters;

    @PostConstruct
    public void init() {
        logger.info("Found logLevelSetters:{}", logLevelSetters.toString());
        String name = com.netflix.discovery.DiscoveryClient.class.getName();
        String level = "ERROR";

        LoggerFactory.getLogger(name).info("Log level changed to [{}]", level);
        for (LogLevelSetter s : logLevelSetters) {
            s.setLoggerLevel(name, level);
        }

        String influxLoggerName = "io.micrometer.influx.InfluxMeterRegistry";
        Logger influxLogger = LoggerFactory.getLogger(influxLoggerName);
        if(!influxLogger.isDebugEnabled()){
            // 如果没有显式启用debug，则禁用influx日志
            LoggerFactory.getLogger(influxLoggerName).info("Log level changed to [{}]", "OFF");
            for (LogLevelSetter s : logLevelSetters) {
                s.setLoggerLevel(influxLoggerName, "OFF");
            }
        }
    }

}
