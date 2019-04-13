package com.bkjk.platform.gray;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.gray.endpoint.GrayEndpoint;

@Configuration
public class GrayEndpointAutoConfiguration {

    @Bean
    public GrayEndpoint grayEndpoint() {
        return new GrayEndpoint();
    }

    @Bean
    public GrayRulesStore grayRulesStore() {
        GrayRulesStore store = new GrayRulesStore();
        return store;
    }
}
