package com.bkjk.platform.rabbit.logger;

import com.bkjk.platform.rabbit.async.AsynchronousFlusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.sql.DataSource;

public class DatabaseMySQLTraceLogger implements TraceLogger {
    public static final Logger logger = LoggerFactory.getLogger(DatabaseMySQLTraceLogger.class);
    private AsynchronousFlusher<MessageTraceBean> asynchronousFlusher;

    public AsynchronousFlusher<MessageTraceBean> getAsynchronousFlusher() {
        return asynchronousFlusher;
    }

    public DatabaseMySQLTraceLogFlushHandler getDatabaseMySQLTraceLogFlushHandler() {
        return databaseMySQLTraceLogFlushHandler;
    }

    private DatabaseMySQLTraceLogFlushHandler databaseMySQLTraceLogFlushHandler;

    public DatabaseMySQLTraceLogger(DataSource dataSource, int capacity) {
        Assert.notNull(dataSource, "DatabaseTraceLogger must have dataSource.");
        Assert.isTrue(capacity > 0, "Capacity must be bigger than ZERO");
        databaseMySQLTraceLogFlushHandler = new DatabaseMySQLTraceLogFlushHandler(dataSource);
        asynchronousFlusher = new AsynchronousFlusher<>(databaseMySQLTraceLogFlushHandler, 1, 2000, 200);
        asynchronousFlusher.start();
    }

    @Override
    public void log(MessageTraceBean trace) {
        asynchronousFlusher.add(trace);
    }
}
