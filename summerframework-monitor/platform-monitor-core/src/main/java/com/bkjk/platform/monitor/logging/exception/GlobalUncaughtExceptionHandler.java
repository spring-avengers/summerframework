package com.bkjk.platform.monitor.logging.exception;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalUncaughtExceptionHandler implements UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalUncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.error("An exception has been raised by Name:{},Id:{},Class:{}", t.getName(), t.getId(), t.getClass(), e);
    }

}
