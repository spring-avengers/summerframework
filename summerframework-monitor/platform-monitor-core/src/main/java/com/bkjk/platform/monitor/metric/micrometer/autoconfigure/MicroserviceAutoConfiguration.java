package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.bkjk.platform.monitor.metric.micrometer.binder.openfeign.OpenfeignMetricsBinder;

import feign.Client;

@AutoConfigureAfter(value = {MicrometerAutoConfiguration.class})
public class MicroserviceAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Client.class)
    static class FeigMeternAutoConfiguration {
        @Bean
        public OpenfeignMetricsBinder openfeignMetrics(PlatformTag platformTag) {
            return new OpenfeignMetricsBinder(platformTag.getTags());
        }
    }

}
