package com.bkjk.platform.eureka.event;

import org.springframework.context.ApplicationEventPublisher;

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.discovery.StatusChangeEvent;

public class EurekaClientEventListener implements EurekaEventListener {
    private ApplicationEventPublisher publisher;

    public EurekaClientEventListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onEvent(EurekaEvent event) {
        if (event instanceof CacheRefreshedEvent) {
            publisher.publishEvent(new EurekaClientLocalCacheRefreshedEvent((CacheRefreshedEvent)event));
        } else if (event instanceof StatusChangeEvent) {
            publisher.publishEvent(new EurekaClientStatusChangeEvent((StatusChangeEvent)event));
        }
    }

}
