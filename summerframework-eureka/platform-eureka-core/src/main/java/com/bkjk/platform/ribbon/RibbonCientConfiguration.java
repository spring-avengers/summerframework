
package com.bkjk.platform.ribbon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.PropertiesFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.ribbon.wrapper.ZoneAvoidanceAndGrayAndLoadBasedRule;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IRule;

@Configuration
@EnableConfigurationProperties
public class RibbonCientConfiguration {
    @Value("${ribbon.client.name}")
    private String name = "client";

    @Autowired
    private PropertiesFactory propertiesFactory;

    @Bean
    @ConditionalOnMissingBean
    public IRule ribbonRule(IClientConfig config) {
        if (this.propertiesFactory.isSet(IRule.class, name)) {
            return this.propertiesFactory.get(IRule.class, config, name);
        }
        ZoneAvoidanceAndGrayAndLoadBasedRule rule = new ZoneAvoidanceAndGrayAndLoadBasedRule();
        rule.initWithNiwsConfig(config);
        return rule;
    }

}
