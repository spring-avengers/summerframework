package com.bkjk.platform.monitor.metric.micrometer.binder.threadpool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.bkjk.platform.monitor.metric.micrometer.PlatformTag;
import com.google.common.collect.Lists;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

public class ThreadPoolBinder implements MeterBinder {
    static class ThreadPoolTaskExecutorMetrics implements MeterBinder {
        private final String name;
        private final Iterable<Tag> tags;

        private final ThreadPoolTaskExecutor executor;

        public ThreadPoolTaskExecutorMetrics(ThreadPoolTaskExecutor executor, String name, Iterable<Tag> tags) {
            this.name = name;
            this.tags = tags;
            this.executor = executor;
        }

        @Override
        public void bindTo(MeterRegistry registry) {
            if (executor == null) {
                return;
            }
            monitor(registry, executor.getThreadPoolExecutor());
        }

        private void monitor(MeterRegistry registry, ThreadPoolExecutor tp) {
            FunctionCounter.builder(name + ".completed", tp, ThreadPoolExecutor::getCompletedTaskCount).tags(tags)
                .description("The approximate total number of tasks that have completed execution").register(registry);

            Gauge.builder(name + ".active", tp, ThreadPoolExecutor::getActiveCount).tags(tags)
                .description("The approximate number of threads that are actively executing tasks").register(registry);

            Gauge.builder(name + ".queued", tp, tpRef -> tpRef.getQueue().size()).tags(tags)
                .description("The approximate number of threads that are queued for execution").register(registry);

            Gauge.builder(name + ".pool", tp, ThreadPoolExecutor::getPoolSize).tags(tags)
                .description("The current number of threads in the pool").register(registry);
        }
    }

    @Autowired(required = false)
    private Map<String, ThreadPoolTaskExecutor> executors;

    @Autowired(required = false)
    private Map<String, ThreadPoolTaskScheduler> schedulerExecutors;

    @Autowired
    PlatformTag platformTag;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        if (executors != null) {
            executors.forEach((beanName, executor) -> {
                List<Tag> tags = Lists.newArrayList(platformTag.getTags());
                tags.add(Tag.of("beanName", beanName));
                new ThreadPoolTaskExecutorMetrics(executor, ThreadPoolTaskExecutor.class.getSimpleName(), tags)
                    .bindTo(meterRegistry);
            });
        }
        if (schedulerExecutors != null) {
            schedulerExecutors.forEach((beanName, executor) -> {
                List<Tag> tags = Lists.newArrayList(platformTag.getTags());
                tags.add(Tag.of("beanName", beanName));
                monitor(ThreadPoolTaskScheduler.class.getSimpleName(), executor.getScheduledThreadPoolExecutor(),
                    meterRegistry, tags);
            });
        }
    }

    private void monitor(String name, ScheduledThreadPoolExecutor tp, MeterRegistry meterRegistry, Iterable<Tag> tags) {
        FunctionCounter.builder(name + ".completed", tp, ScheduledThreadPoolExecutor::getCompletedTaskCount).tags(tags)
            .description("The approximate total number of tasks that have completed execution").register(meterRegistry);
        Gauge.builder(name + ".active", tp, ScheduledThreadPoolExecutor::getActiveCount).tags(tags)
            .description("The approximate number of threads that are actively executing tasks").register(meterRegistry);
        Gauge.builder(name + ".queued", tp, (tpRef) -> {
            return (double)tpRef.getQueue().size();
        }).tags(tags).description("The approximate number of threads that are queued for execution")
            .register(meterRegistry);
        Gauge.builder(name + ".pool", tp, ScheduledThreadPoolExecutor::getPoolSize).tags(tags)
            .description("The current number of threads in the pool").register(meterRegistry);
    }

}
