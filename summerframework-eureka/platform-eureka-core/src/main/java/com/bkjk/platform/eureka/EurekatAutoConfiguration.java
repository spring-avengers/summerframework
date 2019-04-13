
package com.bkjk.platform.eureka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.eureka.log.Log4j2LevelSetter;
import com.bkjk.platform.eureka.log.Log4jLevelSetter;
import com.bkjk.platform.eureka.log.LogLevelInitializer;
import com.bkjk.platform.eureka.log.LogbackLevelSetter;

@Configuration
@ConditionalOnProperty(value = "com.bkjk.platform.restclient.enabled", matchIfMissing = true)
public class EurekatAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "org.apache.logging.log4j.core.Logger")
    public Log4j2LevelSetter log4j2LevelSetter() {
        return new Log4j2LevelSetter();
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.log4j.Logger")
    public Log4jLevelSetter log4jLevelSetter() {
        return new Log4jLevelSetter();
    }

    @Bean
    @ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
    public LogbackLevelSetter logbackLevelSetter() {
        return new LogbackLevelSetter();
    }

    @Bean
    public LogLevelInitializer logLevelInitializer() {
        return new LogLevelInitializer();
    }
}
