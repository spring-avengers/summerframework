package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.KafkaClient;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.bkjk.platform.monitor.metric.micrometer.binder.jms.KafkaProducerMetricsBinder;
import com.bkjk.platform.monitor.metric.micrometer.binder.jms.RabbitMetricsBinder;
import com.bkjk.platform.monitor.metric.micrometer.binder.kafka.KafkaConsumerMetrics;

import io.micrometer.core.instrument.MeterRegistry;

@ConditionalOnClass(com.bkjk.platform.rabbit.RabbitAutoConfiguration.class)
@AutoConfigureAfter(value = {com.bkjk.platform.rabbit.RabbitAutoConfiguration.class})
public class JmsAutoConfiguration {

    @Configuration
    @ConditionalOnClass(KafkaClient.class)
    public static class KafkaMetricsAutoConfiguration {
        @Bean
        public KafkaConsumerMetrics kafkaConsumerMetrics(PlatformTag platformTag) {
            return new KafkaConsumerMetrics(platformTag.getTags());
        }

        @Bean
        public KafkaProducerMetricsBinder kafkaProducerMetrics(PlatformTag platformTag) {
            return new KafkaProducerMetricsBinder(platformTag.getTags());
        }
    }

    public static class RabbitCustomizer {
        @Autowired
        private AbstractConnectionFactory connectionFactory;
        @Autowired
        private PlatformTag platformTag;
        @Autowired
        private MeterRegistry registry;

        @PostConstruct
        public void init() {
            new RabbitMetricsBinder(connectionFactory, platformTag.getTags()).bindTo(registry);
        }
    }

    @Configuration
    @ConditionalOnClass(AbstractConnectionFactory.class)
    public static class RabbitMetricsAutoConfiguration {
        @Bean
        @ConditionalOnBean(AbstractConnectionFactory.class)
        public RabbitCustomizer rabbitCustomizer() {
            return new RabbitCustomizer();
        }

    }
}
