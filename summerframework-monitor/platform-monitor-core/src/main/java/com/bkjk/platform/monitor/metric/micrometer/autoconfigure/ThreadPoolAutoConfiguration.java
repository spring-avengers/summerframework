package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

import com.bkjk.platform.monitor.metric.micrometer.binder.threadpool.ThreadPoolBinder;

@AutoConfigureAfter(MicrometerAutoConfiguration.class)
public class ThreadPoolAutoConfiguration {

    @Bean
    public ThreadPoolBinder threadPoolBinder() {
        return new ThreadPoolBinder();
    }
}
