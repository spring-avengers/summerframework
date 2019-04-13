package com.bkjk.platform.monitor.metric.prometheus;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;

import io.micrometer.prometheus.PrometheusMeterRegistry;

@Configuration
@ConditionalOnWebApplication
public class PrometheusAutoConfiguration {

    public PrometheusActuatorEndpoint prometheusActuatorEndpoint(PrometheusMeterRegistry prometheusMeterRegistry) {
        return new PrometheusActuatorEndpoint(prometheusMeterRegistry);
    }

}
