package com.bkjk.platfrom.dts.core;

import java.util.Map;
import java.util.concurrent.Callable;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

public class SpringCloudDtsContextHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

    static class HystrixContextCallable<S> implements Callable<S> {

        private final Callable<S> callable;
        private final Map<String, String> attachments;

        public HystrixContextCallable(Callable<S> callable) {
            this.attachments = SpringCloudDtsContext.getContext().getAttachments();
            this.callable = callable;
            SpringCloudDtsContext.removeContext();
        }

        @Override
        public S call() throws Exception {
            SpringCloudDtsContext.getContext().setAttachment(attachments);
            return callable.call();
        }
    }

    public SpringCloudDtsContextHystrixConcurrencyStrategy() {
        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();
        HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
        HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
        HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        HystrixPlugins.reset();
        HystrixPlugins.getInstance().registerConcurrencyStrategy(this);
        HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
        HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
    }

    @Override
    public <T> Callable<T> wrapCallable(Callable<T> callable) {
        return new HystrixContextCallable<T>(callable);
    }

}
