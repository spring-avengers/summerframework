package com.bkjk.platform.eureka.wrapper;

import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import com.bkjk.platform.common.Constants;
import com.bkjk.platform.eureka.support.ScheduleReportServerLoad;

public class EurekaServiceRegistryWrapper extends EurekaServiceRegistry {

    private final ServiceRegistry<EurekaRegistration> serviceRegistry;
    private final ConfigurableEnvironment environment;

    public EurekaServiceRegistryWrapper(ServiceRegistry<EurekaRegistration> serviceRegistry,
        ConfigurableEnvironment environment) {
        this.serviceRegistry = serviceRegistry;
        this.environment = environment;
    }

    @Override
    public void close() {
        serviceRegistry.close();
    }

    @Override
    public void deregister(EurekaRegistration registration) {
        serviceRegistry.deregister(registration);
    }

    private String getManagementUrl(EurekaRegistration registration) {
        String sslKey = "server.ssl.enabled";
        String portKey = "server.port";
        String pathKey = "server.servlet.context-path";
        String managementKey = "management.";
        String endpointPathKey = "management.endpoints.web.base-path";
        if (environment.containsProperty(managementKey + portKey)) {
            sslKey = managementKey + sslKey;
            portKey = managementKey + portKey;
            pathKey = managementKey + pathKey;
        }
        Boolean isHttps = environment.getProperty(sslKey, Boolean.class, Boolean.FALSE);
        String port = environment.getProperty(portKey, "");
        String contextPath = environment.getProperty(pathKey, "/");
        String scheme = isHttps ? "https" : "http";
        String uri = String.format("%s://%s:%s", scheme, registration.getHost(),
            StringUtils.isEmpty(port) ? registration.getPort() : port);
        uri = uri + contextPath;
        uri = uri + environment.getProperty(endpointPathKey, "/actuator");
        return uri;
    }

    private String getProperties(String key) {
        return this.environment.getProperty(key);
    }

    @Override
    public Object getStatus(EurekaRegistration registration) {
        return serviceRegistry.getStatus(registration);
    }

    @Override
    public void register(EurekaRegistration registration) {
        String group = this.getProperties(Constants.PROVIDER_INSTANCE_GROUP);
        String version = this.getProperties(Constants.PROVIDER_INSTANCE_VERSION);
        if (group != null && version != null) {
            registration.getMetadata().put(Constants.EUREKA_METADATA_GROUP, group);
            registration.getMetadata().put(Constants.EUREKA_METADATA_VERSION, version);
        }

        // @See platform-starter-springfox need to custom swagger-ui
        // Add by tao.yang 2018/09/18
        String springfoxEnable = this.getProperties(Constants.PROVIDER_SPRINGFOX_ENABLE);
        if ("true".equals(springfoxEnable)) {
            registration.getMetadata().put(Constants.EUREKA_METADATA_SPRINGFOX, springfoxEnable);
        }
        registration.getMetadata().put(Constants.EUREKA_METADATA_MANAGEMENT_URL, getManagementUrl(registration));
        serviceRegistry.register(registration);
        new ScheduleReportServerLoad(registration).start();
    }

    @Override
    public void setStatus(EurekaRegistration registration, String status) {
        serviceRegistry.setStatus(registration, status);
    }

}
