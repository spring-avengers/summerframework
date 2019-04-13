package com.bkjk.platform.monitor.metric.micrometer.binder.db.p6spy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.Loggable;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.logging.Category;
import com.p6spy.engine.logging.LoggingEventListener;
import com.p6spy.engine.logging.P6LogLoadableOptions;
import com.p6spy.engine.logging.P6LogOptions;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.P6Logger;

public class MyLoggingEventListener extends LoggingEventListener {

    public static final MyLoggingEventListener INSTANCE = new MyLoggingEventListener();

    private static final Logger logger = LoggerFactory.getLogger("monitor.sql");
    private static SQLLogger sqlLogger;

    private static final Set<Category> CATEGORIES_IMPLICITLY_INCLUDED =
        new HashSet<Category>(Arrays.asList(Category.ERROR, Category.OUTAGE));

    protected static void doLog(int connectionId, long elapsedNanos, Category category, String prepared, String sql,
        String url, Loggable loggable) {

        final String format = P6SpyOptions.getActiveInstance().getDateformat();
        final String stringNow;
        if (format == null) {
            stringNow = Long.toString(System.currentTimeMillis());
        } else {
            stringNow = new SimpleDateFormat(format).format(new java.util.Date()).trim();
        }

        sqlLogger.logSQL(connectionId, stringNow, TimeUnit.NANOSECONDS.toMillis(elapsedNanos), category, prepared, sql,
            url, loggable);

        final boolean stackTrace = P6SpyOptions.getActiveInstance().getStackTrace();
        if (stackTrace) {
            final String stackTraceClass = P6SpyOptions.getActiveInstance().getStackTraceClass();
            Exception e = new Exception();
            if (stackTraceClass != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stack = sw.toString();
                if (stack.indexOf(stackTraceClass) == -1) {
                    e = null;
                }
            }
            if (e != null) {
                sqlLogger.logException(e);
            }
        }
    }

    private static synchronized void initialize() {
        if (sqlLogger == null) {
            P6Logger instance = P6SpyOptions.getActiveInstance().getAppenderInstance();
            if (instance instanceof SQLLogger) {
                sqlLogger = (SQLLogger)instance;
            } else {
                sqlLogger = new SQLLogger();
            }
        }
    }

    static boolean isCategoryOk(Category category) {
        final P6LogLoadableOptions opts = P6LogOptions.getActiveInstance();
        if (null == opts) {
            return CATEGORIES_IMPLICITLY_INCLUDED.contains(category);
        }

        final Set<Category> excludeCategories = opts.getExcludeCategoriesSet();

        return sqlLogger != null && sqlLogger.isCategoryEnabled(category)
            && (excludeCategories == null || !excludeCategories.contains(category));
    }

    static boolean isLoggable(String sql) {
        if (null == sql) {
            return false;
        }

        final P6LogLoadableOptions opts = P6LogOptions.getActiveInstance();

        if (!opts.getFilter()) {
            return true;
        }

        final Pattern sqlExpressionPattern = opts.getSQLExpressionPattern();
        final Pattern includeExcludePattern = opts.getIncludeExcludePattern();

        return (sqlExpressionPattern == null
            || sqlExpressionPattern != null && sqlExpressionPattern.matcher(sql).matches())
            && (includeExcludePattern == null
                || includeExcludePattern != null && includeExcludePattern.matcher(sql).matches());
    }

    public static void logElapsed(int connectionId, long timeElapsedNanos, Category category, Loggable loggable) {

        if (sqlLogger == null) {
            initialize();
            if (sqlLogger == null) {
                return;
            }
        }

        String sql;
        String url = loggable.getConnectionInformation().getUrl();
        if (logger != null && meetsThresholdRequirement(timeElapsedNanos) && isCategoryOk(category)
            && isLoggable(sql = loggable.getSql())) {
            doLog(connectionId, timeElapsedNanos, category, sql, loggable.getSqlWithValues(), url == null ? "" : url,
                loggable);
        } else if (logger.isDebugEnabled()) {
            sql = loggable.getSqlWithValues();
            logger.debug("P6Spy intentionally did not log category: " + category + ", statement: " + sql
                + "  Reason: logger=" + logger + ", isLoggable=" + isLoggable(sql) + ", isCategoryOk="
                + isCategoryOk(category) + ", meetsTreshold=" + meetsThresholdRequirement(timeElapsedNanos));
        }
    }

    private static boolean meetsThresholdRequirement(long timeTaken) {
        final P6LogLoadableOptions opts = P6LogOptions.getActiveInstance();
        long executionThreshold = null != opts ? opts.getExecutionThreshold() : 0;

        return executionThreshold <= 0 || TimeUnit.NANOSECONDS.toMillis(timeTaken) > executionThreshold;
    }

    public MyLoggingEventListener() {
    }

    @Override
    protected void logElapsed(Loggable loggable, long timeElapsedNanos, Category category, SQLException e) {
        logElapsed(loggable.getConnectionInformation().getConnectionId(), timeElapsedNanos, category, loggable);
    }

    @Override
    public void onAfterAnyAddBatch(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        logElapsed(statementInformation, timeElapsedNanos, Category.BATCH, e);
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        logElapsed(statementInformation, timeElapsedNanos, Category.STATEMENT, e);
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        logElapsed(connectionInformation, timeElapsedNanos, Category.COMMIT, e);
    }

    @Override
    public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos,
        int[] updateCounts, SQLException e) {
        logElapsed(statementInformation, timeElapsedNanos, Category.BATCH, e);
    }

    @Override
    public void onAfterGetResultSet(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        logElapsed(statementInformation, timeElapsedNanos, Category.RESULTSET, e);
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
        if (resultSetInformation.getCurrRow() > -1) {

            resultSetInformation.generateLogMessage();
        }
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value,
        SQLException e) {
        resultSetInformation.setColumnValue(Integer.toString(columnIndex), value);
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value,
        SQLException e) {
        resultSetInformation.setColumnValue(columnLabel, value);
    }

    @Override
    public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext,
        SQLException e) {
        if (hasNext) {
            logElapsed(resultSetInformation, timeElapsedNanos, Category.RESULT, e);
        }
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        logElapsed(connectionInformation, timeElapsedNanos, Category.ROLLBACK, e);
    }

    @Override
    public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) {
        if (resultSetInformation.getCurrRow() > -1) {

            resultSetInformation.generateLogMessage();
        }
    }

}
