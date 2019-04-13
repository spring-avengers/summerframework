package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.bkjk.platform.monitor.metric.micrometer.binder.resttemplate.MetricsClientHttpRequestInterceptor;
import com.bkjk.platform.monitor.metric.micrometer.binder.resttemplate.RestTemplatePostProcessor;

import io.micrometer.core.instrument.MeterRegistry;

@AutoConfigureAfter(MicrometerAutoConfiguration.class)
public class RestTemplateAutoConfiguration {

    @Configuration
    @ConditionalOnClass({ClientHttpRequestInterceptor.class, RestTemplate.class})
    public static class WebConfig {
        @Bean
        public SmartInitializingSingleton metricsAsyncRestTemplateInitializer(
            final ObjectProvider<List<AsyncRestTemplate>> asyncRestTemplatesProvider,
            final RestTemplatePostProcessor customizer) {
            return () -> {
                final List<AsyncRestTemplate> asyncRestTemplates = asyncRestTemplatesProvider.getIfAvailable();
                if (!CollectionUtils.isEmpty(asyncRestTemplates)) {
                    asyncRestTemplates.forEach(customizer::customize);
                }
            };
        }

        @Bean
        public MetricsClientHttpRequestInterceptor metricsClientHttpRequestInterceptor(MeterRegistry meterRegistry) {
            return new MetricsClientHttpRequestInterceptor(meterRegistry, METRIC_NAME);
        }

        @Bean
        public SmartInitializingSingleton metricsRestTemplateInitializer(
            final ObjectProvider<List<RestTemplate>> restTemplatesProvider,
            final RestTemplatePostProcessor customizer) {
            return () -> {
                final List<RestTemplate> restTemplates = restTemplatesProvider.getIfAvailable();
                if (!CollectionUtils.isEmpty(restTemplates)) {
                    restTemplates.forEach(customizer::customize);
                }
            };
        }

        @Bean
        public RestTemplatePostProcessor
            restTemplatePostProcessor(MetricsClientHttpRequestInterceptor metricsClientHttpRequestInterceptor) {
            return new RestTemplatePostProcessor(metricsClientHttpRequestInterceptor);
        }
    }

    public static final String METRIC_NAME = "rest_template_timer";

}
