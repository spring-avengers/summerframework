package com.bkjk.platfrom.dts.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bkjk.platfrom.dts.core.client.aop.DtsTransactionScaner;
import com.bkjk.platfrom.dts.core.interceptor.DtsRemoteInterceptor;
import com.bkjk.platfrom.dts.core.lb.DtsLoadbalance;
import com.netflix.hystrix.HystrixCommand;

import feign.RequestInterceptor;

@Configuration
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class DtsAutoConfiguration {

    @Configuration
    @ConditionalOnBean(DtsTransactionScaner.class)
    protected class ConsumerContextSupportConfig {

        @Bean
        @ConditionalOnClass(HystrixCommand.class)
        public SpringCloudDtsContextHystrixConcurrencyStrategy contextHystrixConcurrencyStrategy() {
            return new SpringCloudDtsContextHystrixConcurrencyStrategy();
        }

        @Bean
        public RequestInterceptor requestInterceptor() {
            return new DtsRemoteInterceptor();
        }

    }

    @Value("${server.error.path:${error.path:/error}}")
    private String errorPath;

    @Configuration
    protected class ProviderContextSupportConfig implements WebMvcConfigurer {
        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new DtsRemoteInterceptor())//
                .addPathPatterns("/**").excludePathPatterns("/**" + errorPath);
        }
    }

    @Bean
    public DtsLoadbalance dtsLoadbalance(DiscoveryClient discoveryClient) {
        return new DtsLoadbalance(discoveryClient);
    }

}
