package com.bkjk.platform.monitor.metric.micrometer.binder.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MysqlStatusBinder implements MeterBinder {

    static class QueryToDoubleFunction implements ToDoubleFunction<DataSource> {
        private String query;

        public QueryToDoubleFunction(String query) {
            this.query = query;
        }

        @Override
        public double applyAsDouble(DataSource ds) {
            try (Connection conn = ds.getConnection(); PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(2);
            } catch (SQLException ignored) {
                return 0;
            }
        }
    }

    private final DataSource dataSource;
    private final String query = "show global status like '%s'";
    private final String dataSourceName;
    private final Iterable<Tag> tags;
    private final Set<String> variableNames;

    private boolean wrongType;

    public MysqlStatusBinder(DataSource dataSource, String dataSourceName, Iterable<Tag> tags,
        Set<String> variableNames) {
        this.dataSource = dataSource;
        this.dataSourceName = dataSourceName;
        this.tags = tags;
        this.variableNames = variableNames.stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        variableNames.forEach((k) -> {
            String querySql = String.format(query, k);
            Gauge.builder(String.format("mysql.global.status.%s", k), dataSource, new QueryToDoubleFunction(querySql))
                .tags(tags).tag("db", dataSourceName).description("MySQL global status").register(registry);

        });
    }

    public boolean isWrongType() {
        return wrongType;
    }
}
