package com.bkjk.platform.rabbit.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jTraceLogger implements TraceLogger {

    public static final Slf4jTraceLogger instance = new Slf4jTraceLogger();

    private static final Logger TRACELOGGER = LoggerFactory.getLogger("traceLog");

    @Override
    public void log(MessageTraceBean trace) {
        TRACELOGGER.info(trace.toString());
    }
}
