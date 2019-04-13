package com.bkjk.platform.gray.hystrix;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class RequestAttributesDelegatingCallable<V> implements Callable, Runnable {

    private final static Logger logger = LoggerFactory.getLogger(RequestAttributesDelegatingCallable.class);

    public static <V> Callable<V> create(Callable<V> delegate, RequestAttributes requestAttributes) {
        return requestAttributes == null ? new RequestAttributesDelegatingCallable<V>(delegate)
            : new RequestAttributesDelegatingCallable<V>(delegate, requestAttributes);
    }

    private final Callable<V> delegate;

    private final RequestAttributes requestAttributes;

    private RequestAttributes originalRequestAttributes;

    private RequestAttributesDelegatingCallable(Callable<V> delegate) {
        Assert.notNull(delegate, "delegate cannot be null");
        this.delegate = delegate;
        this.requestAttributes = RequestContextHolder.getRequestAttributes();
    }

    private RequestAttributesDelegatingCallable(Callable<V> delegate, RequestAttributes requestAttributes) {
        Assert.notNull(delegate, "delegate cannot be null");
        Assert.notNull(requestAttributes, "requestAttributes cannot be null");
        this.delegate = delegate;
        this.requestAttributes = requestAttributes;
    }

    @Override
    public V call() throws Exception {
        this.originalRequestAttributes = RequestContextHolder.getRequestAttributes();

        try {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            logger.info("requestAttributes has been delegated by {}", requestAttributes);
            return delegate.call();
        } finally {
            RequestContextHolder.setRequestAttributes(originalRequestAttributes);
            this.originalRequestAttributes = null;
        }
    }

    @Override
    public void run() {
        try {
            this.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
