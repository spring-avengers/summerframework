package com.bkjk.platform.eureka.event;

import org.springframework.context.ApplicationEvent;

import com.netflix.discovery.CacheRefreshedEvent;

public class EurekaClientLocalCacheRefreshedEvent extends ApplicationEvent {
    private CacheRefreshedEvent source;

    public EurekaClientLocalCacheRefreshedEvent(CacheRefreshedEvent source) {
        super(source);
    }

    @Override
    public CacheRefreshedEvent getSource() {
        return source;
    }
}
