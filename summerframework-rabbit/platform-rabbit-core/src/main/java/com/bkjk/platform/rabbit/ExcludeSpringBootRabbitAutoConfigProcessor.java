
package com.bkjk.platform.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import com.bkjk.platform.common.spring.SpringAutoConfigurationUtil;

public class ExcludeSpringBootRabbitAutoConfigProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String RABBIT_AUTOCONFIGURATION =
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration";

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        SpringAutoConfigurationUtil.excludeAutoConfiguration(environment, application, RABBIT_AUTOCONFIGURATION);
    }
}
