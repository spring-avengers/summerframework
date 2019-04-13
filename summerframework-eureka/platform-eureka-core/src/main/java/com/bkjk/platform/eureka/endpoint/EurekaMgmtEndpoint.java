
package com.bkjk.platform.eureka.endpoint;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bkjk.platform.eureka.EurekaRuleCache;

@ManagedResource(description = "Can be used to manager service discovery")
@SuppressWarnings("unchecked")

@RestControllerEndpoint(id = "eurekamgmt")
public class EurekaMgmtEndpoint implements ApplicationContextAware, EnvironmentAware {

    public static final String ENDPOINT_SERVICE_MGMT_ENABLED_PROPERTY = "endpoints.service-discovery-mgmt.enabled";

    public static final String ENDPOINT_SERVICE_MGMT_SENSITIVE_PROPERTY = "endpoints.service-discovery-mgmt.sensitive";

    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaMgmtEndpoint.class);

    private ConfigurableApplicationContext context;

    private Environment environment;

    private final ServiceRegistry serviceRegistry;

    private final EurekaRuleCache eurekaRuleCache = EurekaRuleCache.getInstance();

    private Registration registration;

    public EurekaMgmtEndpoint(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @RequestMapping(path = "clearRoute", method = RequestMethod.GET)
    @ManagedOperation
    public Object clearDynamicsRoute(@RequestParam("serviceId") String serviceId) {
        if (!this.isEnabled()) {
            return new ResponseEntity<>(Collections.singletonMap("message", "This endpoint is disabled"),
                HttpStatus.NOT_FOUND);
        }
        Assert.notNull(serviceId, "serviceId may not by null");
        if (this.registration == null) {
            throw new RuntimeException("No registration found.");
        }
        this.eurekaRuleCache.clear(serviceId);
        LOGGER.info("ServiceDiscoveryMgmtEndpoint, Clear for service id = {}", serviceId);
        return "success";
    }

    @RequestMapping(path = "deregister", method = RequestMethod.POST)
    @ResponseBody
    @ManagedOperation
    public Object deregister() {
        if (!this.isEnabled()) {
            return new ResponseEntity<>(Collections.singletonMap("message", "This endpoint is disabled"),
                HttpStatus.NOT_FOUND);
        }
        if (this.registration == null) {
            throw new RuntimeException("No registration found.");
        }

        this.serviceRegistry.deregister(this.registration);
        LOGGER.info("ServiceDiscoveryMgmtEndpoint, deregister for service id  = {}", registration.getServiceId());
        return "success";
    }

    private boolean isEnabled() {
        if (environment.containsProperty(ENDPOINT_SERVICE_MGMT_ENABLED_PROPERTY)) {
            return environment.getProperty(ENDPOINT_SERVICE_MGMT_ENABLED_PROPERTY, Boolean.class);
        }
        return true;
    }

    @RequestMapping(path = "shutdown", method = RequestMethod.POST)
    @ResponseBody
    @ManagedOperation
    public Object offline(@RequestParam(value = "shutdown", required = false, defaultValue = "true") boolean shutdown) {
        if (!this.isEnabled()) {
            return new ResponseEntity<>(Collections.singletonMap("message", "This endpoint is disabled"),
                HttpStatus.NOT_FOUND);
        }
        this.serviceRegistry.close();
        LOGGER.info("ServiceDiscoveryMgmtEndpoint, finish close");
        if (shutdown) {
            if (this.context == null) {
                throw new RuntimeException("Success close service registry but no context to shutdown.");
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException ex) {

                    }
                    LOGGER.info("ServiceDiscoveryMgmtEndpoint, start shutdown app, bye...");
                    EurekaMgmtEndpoint.this.context.close();
                }
            }).start();
        }
        return "success";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            this.context = (ConfigurableApplicationContext)applicationContext;
        }
    }

    @RequestMapping(path = "dynamicsroute", method = RequestMethod.GET)
    @ManagedOperation
    public Object setDynamicsRoute(@RequestParam("serviceId") String serviceId,
        @RequestParam("routeIp") String routeIp) {
        if (!this.isEnabled()) {
            return new ResponseEntity<>(Collections.singletonMap("message", "This endpoint is disabled"),
                HttpStatus.NOT_FOUND);
        }
        Assert.notNull(serviceId, "serviceId may not by null");
        Assert.notNull(routeIp, "routeIp may not by null");
        if (this.registration == null) {
            throw new RuntimeException("No registration found.");
        }
        this.eurekaRuleCache.put(serviceId, routeIp);
        LOGGER.info("ServiceDiscoveryMgmtEndpoint, DynamicsRoute for service id = {}, routeIp = {}", serviceId,
            routeIp);
        return "success";
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    @RequestMapping(path = "instance-status", method = RequestMethod.POST)
    @ResponseBody
    @ManagedOperation
    public Object setStatus(@RequestBody String status) {
        if (!this.isEnabled()) {
            return new ResponseEntity<>(Collections.singletonMap("message", "This endpoint is disabled"),
                HttpStatus.NOT_FOUND);
        }
        Assert.notNull(status, "status may not by null");
        if (this.registration == null) {
            throw new RuntimeException("No registration found.");
        }
        this.serviceRegistry.setStatus(this.registration, status);
        LOGGER.info("ServiceDiscoveryMgmtEndpoint, set status for service id = {}, status = {}",
            registration.getServiceId(), status);
        return "success";
    }

}
