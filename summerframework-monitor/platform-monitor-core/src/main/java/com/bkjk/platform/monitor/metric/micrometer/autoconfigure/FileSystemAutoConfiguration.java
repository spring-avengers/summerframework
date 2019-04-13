package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.io.File;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

import com.bkjk.platform.monitor.metric.micrometer.binder.jvm.DiskSpaceMetrics;

@AutoConfigureAfter(MicrometerAutoConfiguration.class)
public class FileSystemAutoConfiguration {

    @Bean
    public DiskSpaceMetrics diskSpaceMetrics() {
        return new DiskSpaceMetrics(new File("."));
    }
}
