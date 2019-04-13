package com.bkjk.platform.monitor.metric.micrometer.binder.db.p6spy;

import com.p6spy.engine.common.Loggable;
import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.FormattedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLLogger extends FormattedLogger {
    private Logger log;

    public SQLLogger() {
        log = LoggerFactory.getLogger("monitor.sql");
    }

    private void doLog(String msg, Category category) {
        if (Category.ERROR.equals(category)) {
            log.error(msg);
        } else if (Category.WARN.equals(category)) {
            log.warn(msg);
        } else if (Category.DEBUG.equals(category)) {
            log.debug(msg);
        } else {
            log.info(msg);
        }
    }

    @Override
    public boolean isCategoryEnabled(Category category) {
        if (Category.ERROR.equals(category)) {
            return log.isErrorEnabled();
        } else if (Category.WARN.equals(category)) {
            return log.isWarnEnabled();
        } else if (Category.DEBUG.equals(category)) {
            return log.isDebugEnabled();
        } else {
            return log.isInfoEnabled();
        }
    }

    @Override
    public void logException(Exception e) {
        log.info("", e);
    }

    @Override
    public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql,
        String url) {
        final String msg = strategy.formatMessage(connectionId, now, elapsed, category.toString(), prepared, sql, url);
        doLog(msg, category);
    }

    public void logSQL(int connectionId, String now, long elapsed, Category category, String prepared, String sql,
        String url, Loggable loggable) {
        if (sql.startsWith("/* ping */")) {
            // 不打印 select 1 这样的心跳SQL
            return;
        }
        final String msg = strategy.formatMessage(connectionId, now, elapsed, category.toString(), prepared, sql, url);
        String isAutoCommit;
        try {
            isAutoCommit = String.valueOf(loggable.getConnectionInformation().getConnection().getAutoCommit());
        } catch (SQLException e) {
            isAutoCommit = "error";
        }
        String isReadOnly;
        try {
            isReadOnly = String.valueOf(loggable.getConnectionInformation().getConnection().isReadOnly());
        } catch (SQLException e) {
            isReadOnly = "error";
        }
        String isolation;
        try {
            int isolationInt = loggable.getConnectionInformation().getConnection().getTransactionIsolation();
            switch (isolationInt) {
                case Connection.TRANSACTION_NONE:
                    isolation = "NONE";
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    isolation = "READ_UNCOMMITTED";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    isolation = "READ_COMMITTED";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    isolation = "REPEATABLE_READ";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    isolation = "SERIALIZABLE";
                    break;
                default:
                    isolation = "KNOWN";
            }

        } catch (SQLException e) {
            isolation = "error";
        }
        doLog(String.format("autocommit(%s)|readonly(%s)|%s|%s", isAutoCommit, isReadOnly, isolation, msg), category);
    }

    @Override
    public void logText(String text) {
        log.info(text);
    }

}
