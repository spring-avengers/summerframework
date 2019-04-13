package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.lang.reflect.Field;
import java.util.List;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import com.bkjk.platform.monitor.MonitorConfigSpringApplicationRunListener;
import com.bkjk.platform.monitor.metric.micrometer.binder.db.DruidDataSourcePoolMetadata;
import com.bkjk.platform.monitor.metric.micrometer.binder.db.P6DataSourceBeanPostProcessor;
import com.bkjk.platform.monitor.metric.micrometer.binder.db.p6spy.SQLLogger;
import com.p6spy.engine.spy.P6DataSource;

@AutoConfigureAfter({MicrometerAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class DataSourceMetricAutoConfiguration {

    @Configuration
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnClass(com.alibaba.druid.pool.DruidDataSource.class)
    static class DruidDataSourceMetricAutoConfiguration {
        @Bean
        public DataSourcePoolMetadataProvider dataSourcePoolMetadataProvider() {
            return new MetricDataSourcePoolMetadataProvider();
        }
    }

    public static class MetricDataSourcePoolMetadataProvider implements DataSourcePoolMetadataProvider {

        @Override
        public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
            if (dataSource instanceof com.alibaba.druid.pool.DruidDataSource) {
                return new DruidDataSourcePoolMetadata((com.alibaba.druid.pool.DruidDataSource)dataSource);
            } else {
                return null;
            }
        }
    }

    @Configuration
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnClass(com.p6spy.engine.spy.P6DataSource.class)
    static class P6DataSourceAutoConfiguration {
        public static final String APPENDER = "p6spy.config.appender";

        public static final String CUSTOM_LOG_MESSAGE_FORMAT = "p6spy.config.customLogMessageFormat";

        public static final String LOG_MESSAGE_FORMAT = "p6spy.config.logMessageFormat";
        public static final String EXECUTION_THRESHOLD = "p6spy.config.executionThreshold";
        public static final String OUTAGE_DETECTION = "p6spy.config.outagedetection";
        public static final String OUTAGE_DETECTION_INTERVAL = "p6spy.config.outagedetectioninterval";
        static {

            System.setProperty(APPENDER, "com.bkjk.platform.monitor.metric.micrometer.binder.db.p6spy.SQLLogger");
            System.setProperty(CUSTOM_LOG_MESSAGE_FORMAT,
                "%(executionTime)ms|%(category)|connection%(connectionId)|%(effectiveSqlSingleLine)");
            System.setProperty(LOG_MESSAGE_FORMAT, "com.p6spy.engine.spy.appender.CustomLineFormat");
            System.setProperty(OUTAGE_DETECTION, "true");

            boolean showSqlWithRealParameter = Boolean.valueOf(
                MonitorConfigSpringApplicationRunListener.getConfig("MONITOR.SQL.WITH-REAL-PARAMETER", "true"));

            int executionThreshold = Integer
                .valueOf(MonitorConfigSpringApplicationRunListener.getConfig("MONITOR.SQL.EXECUTIONTHRESHOLD", "0"));;

            int outagedetectioninterval = Integer.valueOf(
                MonitorConfigSpringApplicationRunListener.getConfig("MONITOR.SQL.OUTAGEDETECTIONINTERVAL", "5"));;

            if (showSqlWithRealParameter) {
                System.setProperty(CUSTOM_LOG_MESSAGE_FORMAT,
                    "%(executionTime)ms|%(category)|connection%(connectionId)|%(sqlSingleLine)");
            }
            if (executionThreshold > 0) {
                System.setProperty(EXECUTION_THRESHOLD, String.valueOf(executionThreshold));
            } else {
                System.setProperty(EXECUTION_THRESHOLD, String.valueOf(0));
            }
            if (outagedetectioninterval > 0) {
                System.setProperty(OUTAGE_DETECTION_INTERVAL, String.valueOf(outagedetectioninterval));
            } else {
                System.setProperty(EXECUTION_THRESHOLD, String.valueOf(5));
            }
        }

        @Bean
        public P6DataSourceBeanPostProcessor dataSourceBeanPostProcessor() {
            return new P6DataSourceBeanPostProcessor();
        }

        @Bean
        @ConditionalOnBean(DataSourcePoolMetadataProvider.class)
        public DataSourcePoolMetadataProvider
            p6DataSourcePoolMetadataProvider(List<DataSourcePoolMetadataProvider> allDataSourcePoolMetadataProvider) {
            return new DataSourcePoolMetadataProvider() {
                @Override
                public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
                    if (dataSource instanceof P6DataSource) {
                        Field realDataSourceField = ReflectionUtils.findField(P6DataSource.class, "realDataSource");
                        ReflectionUtils.makeAccessible(realDataSourceField);
                        try {
                            CommonDataSource ds = (CommonDataSource)realDataSourceField.get(dataSource);
                            if (ds instanceof DataSource) {
                                dataSource = (DataSource)ds;
                                logger.info("realDataSource is {}", ds.getClass());
                            }
                        } catch (IllegalAccessException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                    for (DataSourcePoolMetadataProvider d : allDataSourcePoolMetadataProvider) {
                        DataSourcePoolMetadata dataSourcePoolMetadata = d.getDataSourcePoolMetadata(dataSource);
                        if (null != dataSourcePoolMetadata) {
                            return dataSourcePoolMetadata;
                        }
                    }
                    return null;
                }
            };
        }

        @Bean
        public SQLLogger sqlLogger() {
            return new SQLLogger();
        }
    }

    public static final Logger logger = LoggerFactory.getLogger(DataSourceMetricAutoConfiguration.class);

}
