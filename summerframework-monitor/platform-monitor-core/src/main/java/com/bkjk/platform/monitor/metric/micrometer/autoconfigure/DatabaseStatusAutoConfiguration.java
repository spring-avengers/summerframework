package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.bkjk.platform.monitor.metric.micrometer.binder.db.MysqlStatusBinder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@AutoConfigureAfter(MicrometerAutoConfiguration.class)
public class DatabaseStatusAutoConfiguration {
    @Configuration
    @ConditionalOnBean(DataSource.class)
    public static class MysqlDatabaseStatusAutoConfiguration {

        public static class MysqlStatusBinders implements MeterBinder {

            @Autowired
            PlatformTag platformTag;

            @Autowired
            Map<String, DataSource> dataSourceMap;

            Set<String> variableNames;

            public MysqlStatusBinders(Set<String> variableNames) {
                this.variableNames = variableNames;
            }

            @Override
            public void bindTo(MeterRegistry registry) {
                dataSourceMap.forEach((name, dataSource) -> {
                    new MysqlStatusBinder(dataSource, name, platformTag.getTags(), variableNames).bindTo(registry);
                });
            }
        }

        @Value("${monitor.sql.mysql.variable:}")
        private String mysqlVariable;

        public MysqlStatusBinders mysqlStatusBinders() {
            if (StringUtils.isEmpty(mysqlVariable)) {
                return new MysqlStatusBinders(
                    Arrays.asList(MYSQL_VARIABLES.split(",")).stream().collect(Collectors.toSet()));
            } else {
                return new MysqlStatusBinders(
                    Arrays.asList(mysqlVariable.split(",")).stream().collect(Collectors.toSet()));
            }
        }

    }

    public static final String MYSQL_VARIABLES = "Questions,Com_commit,Com_rollback,Table_locks_waited";
}
