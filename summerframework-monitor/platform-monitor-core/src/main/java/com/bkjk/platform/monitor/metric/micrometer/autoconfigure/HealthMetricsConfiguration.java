package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.List;

public class HealthMetricsConfiguration implements DisposableBean {

    private static boolean running = false;

    public static synchronized void running() {
        running = true;
    }

    private static int getStatusCode(HealthIndicator healthIndicator) {
        if (!running) {
            return 0;
        }
        switch (healthIndicator.health().getStatus().getCode()) {
            case "UP":
                return 3;
            case "OUT_OF_SERVICE":
                return 2;
            case "DOWN":
                return 1;
            case "UNKNOWN":
            default:
                return 0;
        }
    }

    private CompositeHealthIndicator healthIndicator;

    public HealthMetricsConfiguration(HealthAggregator healthAggregator, List<HealthIndicator> healthIndicators,
        MeterRegistry registry, PlatformTag platformTag) {
        healthIndicator = new CompositeHealthIndicator(healthAggregator);
        healthIndicators.forEach(h -> {
            registry.gauge("health." + h.getClass().getSimpleName().replace("HealthIndicator", "").toLowerCase(),
                platformTag.getTags(), h, HealthMetricsConfiguration::getStatusCode);
            healthIndicator.addHealthIndicator(h.toString(), h);
        });
        registry.gauge("health", platformTag.getTags(), healthIndicator, HealthMetricsConfiguration::getStatusCode);
    }

    @Override
    public void destroy() throws Exception {
        running = false;
    }
}
