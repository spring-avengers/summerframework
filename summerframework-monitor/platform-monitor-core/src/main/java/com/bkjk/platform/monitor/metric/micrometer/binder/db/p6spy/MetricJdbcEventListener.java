package com.bkjk.platform.monitor.metric.micrometer.binder.db.p6spy;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.bkjk.platform.monitor.metric.MicrometerUtil;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.Loggable;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.event.SimpleJdbcEventListener;
import com.p6spy.engine.logging.Category;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import net.sf.jsqlparser.parser.CCJSqlParserManager;

public class MetricJdbcEventListener extends SimpleJdbcEventListener {
    public static class SqlParser {

        private static final CCJSqlParserManager PARSER_MANAGER = new CCJSqlParserManager();

        public static String getSqlType(Category category, String sql) {
            if (StringUtils.isEmpty(sql)) {
                return category.getName();
            }
            final Reader sqlReader = new StringReader(sql);
            try {
                net.sf.jsqlparser.statement.Statement stmt = PARSER_MANAGER.parse(sqlReader);
                return stmt.getClass().getSimpleName().toLowerCase();
            } catch (Throwable ignore) {
                logger.info("Error parsing SQL {}", sql);
                return category.getName();
            } finally {
                if (Objects.nonNull(sqlReader)) {
                    try {
                        sqlReader.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }

        }

    }

    public static final Logger logger = LoggerFactory.getLogger(MetricJdbcEventListener.class);
    public static final String SQL_EXECUTE_TIME = "sql_execute_time";

    public static final MetricJdbcEventListener INSTANCE = new MetricJdbcEventListener();

    private static String getHost(String url) {
        if (StringUtils.isEmpty(url)) {
            return "none";
        }
        try {
            return url.split("\\?")[0];
        } catch (Throwable ex) {
            return url;
        }
    }

    private static String getOrDefault(Callable<String> call, String defaultValue) {
        try {
            return call.call();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void main(String[] args) {
        System.out.println(SqlParser.getSqlType(Category.STATEMENT, "deletes a from b"));
        System.out.println(SqlParser.getSqlType(Category.STATEMENT, "insert into a(1)"));
        System.out.println(SqlParser.getSqlType(Category.STATEMENT, "select a from b"));
        System.out.println(SqlParser.getSqlType(Category.STATEMENT, "update a set b=1"));
        System.out.println(SqlParser.getSqlType(Category.STATEMENT, "delete a from b"));
        System.out.println(SqlParser.getSqlType(Category.STATEMENT, "show global status like'Com_rollback'; "));
        System.out.println(SqlParser.getSqlType(Category.COMMIT, " "));

    }

    public MetricJdbcEventListener() {
    }

    @Override
    public void onAfterAnyAddBatch(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        record(statementInformation, timeElapsedNanos, Category.BATCH, e);
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        record(statementInformation, timeElapsedNanos, Category.STATEMENT, e);
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        record(connectionInformation, timeElapsedNanos, Category.COMMIT, e);
    }

    @Override
    public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos,
        int[] updateCounts, SQLException e) {
        record(statementInformation, timeElapsedNanos, Category.BATCH, e);
    }

    @Override
    public void onAfterGetResultSet(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        record(statementInformation, timeElapsedNanos, Category.RESULTSET, e);
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
        record(connectionInformation, timeElapsedNanos, Category.ROLLBACK, e);
    }

    protected void record(Loggable loggable, long timeElapsedNanos, Category category, SQLException e) {
        try {
            Timer timer =
                Metrics
                    .timer(
                        SQL_EXECUTE_TIME, Tags
                            .of(Tag.of("category", category.getName()), Tag.of("readonly", getOrDefault(
                                () -> String.valueOf(loggable.getConnectionInformation().getConnection().isReadOnly()),
                                "error")),
                                Tag.of("autocommit",
                                    getOrDefault(
                                        () -> String.valueOf(
                                            loggable.getConnectionInformation().getConnection().getAutoCommit()),
                                        "error")),
                                Tag.of("statement", SqlParser.getSqlType(category, loggable.getSqlWithValues())),
                                Tag.of("jdbc",
                                    getHost(
                                        loggable.getConnectionInformation().getConnection().getMetaData().getURL())))
                            .and(MicrometerUtil.exceptionAndStatusKey(e)));
            timer.record(timeElapsedNanos, TimeUnit.NANOSECONDS);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
