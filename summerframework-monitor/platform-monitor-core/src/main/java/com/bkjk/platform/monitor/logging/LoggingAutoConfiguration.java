package com.bkjk.platform.monitor.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.monitor.logging.aop.GenericControllerAspect;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(value = "platform.monitor.log.enable",matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    public GenericControllerAspect genericControllerAspect() {
        return new GenericControllerAspect();
    }

}
