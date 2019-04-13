
package com.bkjk.platform.eureka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.jmx.JmxEndpointProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.bkjk.platform.eureka.endpoint.EurekaMgmtEndpoint;

@Configuration
@ConditionalOnProperty(value = "com.bkjk.platform.restclient.enabled", matchIfMissing = true)
@AutoConfigureAfter(EurekaClientAutoConfiguration.class)
public class EurekaMgmtEndpointAutoConfiguration {
    @ConditionalOnBean(ServiceRegistry.class)
    @ConditionalOnClass(Endpoint.class)
    protected static class EurekaMgmtEndpointConfiguration {
        @Autowired(required = false)
        private Registration registration;

        public EurekaMgmtEndpointConfiguration() {
        }

        @Bean
        @ConditionalOnEnabledEndpoint
        public EurekaMgmtEndpoint serviceDiscoveryMgmtEndpoint(ServiceRegistry serviceRegistry,
            Environment environment) {
            EurekaMgmtEndpoint endpoint = new EurekaMgmtEndpoint(serviceRegistry);
            endpoint.setRegistration(registration);
            JmxEndpointProperties r;
            return endpoint;
        }
    }

    public EurekaMgmtEndpointAutoConfiguration() {
    }

}
