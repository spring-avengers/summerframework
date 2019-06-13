package com.bkjk.platform.dts.server.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import com.hazelcast.spi.discovery.integration.DiscoveryService;
import com.hazelcast.spi.discovery.integration.DiscoveryServiceProvider;
import com.hazelcast.spi.discovery.integration.DiscoveryServiceSettings;
import com.netflix.appinfo.ApplicationInfoManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
public class HazelcastConfig {

    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfig.class);

    @Value("${spring.application.name}")
    private String clusterName;
    @Value("${hazelcast.version:1.0.0}")
    private String hazelcastVersion;

    private static final String HAZELCAST_VERSION_KEY = "hazelcast.version";
    private static final String HAZELCAST_HOST_KEY = "hazelcast.host";
    private static final String HAZELCAST_PORT_KEY = "hazelcast.port";

    @Autowired
    EurekaRegistration eurekaRegistration;
    @Autowired
    ApplicationInfoManager applicationInfoManager;

    @Bean
    public Config config(DiscoveryServiceProvider discoveryServiceProvider) {
        Config config = new Config();
        config.getGroupConfig().setName(clusterName.toUpperCase());
        config.setProperty("hazelcast.discovery.enabled", Boolean.TRUE.toString());
        JoinConfig joinConfig = config.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getDiscoveryConfig().setDiscoveryServiceProvider(discoveryServiceProvider);
        config.getMapConfigs().put(CacheConstant.COMMINTING_GLOBALLOG_CACHE,
            new MapConfig(CacheConstant.COMMINTING_GLOBALLOG_CACHE).setBackupCount(2));
        config.getMapConfigs().put(CacheConstant.ROLLBACKING_GLOBALLOG_CACHE,
            new MapConfig(CacheConstant.ROLLBACKING_GLOBALLOG_CACHE).setBackupCount(2));

        Map<String, String> map = applicationInfoManager.getInfo().getMetadata();
        eurekaRegistration.getMetadata().put(HAZELCAST_VERSION_KEY, hazelcastVersion);
        eurekaRegistration.getMetadata().put(HAZELCAST_HOST_KEY, System.getProperty(HAZELCAST_HOST_KEY));
        eurekaRegistration.getMetadata().put(HAZELCAST_PORT_KEY, System.getProperty(HAZELCAST_PORT_KEY));
        map.putAll(eurekaRegistration.getMetadata());
        applicationInfoManager.registerAppMetadata(map);
        return config;
    }

    @Bean
    public DiscoveryServiceProvider discoveryServiceProvider(DiscoveryClient discoveryClient) {
        return new DiscoveryServiceProvider() {

            @Override
            public DiscoveryService newDiscoveryService(DiscoveryServiceSettings settings) {
                return new DiscoveryService() {

                    @Override
                    public void destroy() {
                    }

                    @Override
                    public Map<String, Object> discoverLocalMetadata() {
                        return Collections.emptyMap();
                    }

                    @Override
                    public Iterable<DiscoveryNode> discoverNodes() {
                        List<DiscoveryNode> nodes = new ArrayList<>();
                        discoveryClient.getInstances(clusterName).forEach((ServiceInstance serviceInstance) -> {
                            try {
                                String host = serviceInstance.getMetadata().get(HAZELCAST_HOST_KEY);
                                String port = serviceInstance.getMetadata().get(HAZELCAST_PORT_KEY);
                                String version = serviceInstance.getMetadata().get(HAZELCAST_VERSION_KEY);
                                if (host != null && port != null && StringUtils.equals(version, hazelcastVersion)) {
                                    Address address = new Address(host, Integer.parseInt(port));
                                    DiscoveryNode discoveryNode = new SimpleDiscoveryNode(address);
                                    nodes.add(discoveryNode);
                                }
                            } catch (Exception e) {
                                logger.error("discoverNodes()", e);
                            }
                        });
                        return nodes;
                    }

                    @Override
                    public void start() {
                    }

                };
            }

        };
    }

}
