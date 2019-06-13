package com.bkjk.platform.rabbit.logger;

public class NoopTraceLogger implements TraceLogger {
    public static final NoopTraceLogger instance = new NoopTraceLogger();

    @Override
    public void log(MessageTraceBean trace) {

    }
}
