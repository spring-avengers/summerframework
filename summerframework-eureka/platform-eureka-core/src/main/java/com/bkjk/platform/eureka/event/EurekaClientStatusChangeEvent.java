package com.bkjk.platform.eureka.event;

import org.springframework.context.ApplicationEvent;

import com.netflix.discovery.StatusChangeEvent;

public class EurekaClientStatusChangeEvent extends ApplicationEvent {
    private StatusChangeEvent source;

    public EurekaClientStatusChangeEvent(StatusChangeEvent source) {
        super(source);
    }

    @Override
    public StatusChangeEvent getSource() {
        return source;
    }
}
