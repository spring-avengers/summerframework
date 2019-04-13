
package com.bkjk.platform.ribbon.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bkjk.platform.common.Constants;
import com.bkjk.platform.common.ServerLoadStatus;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.bkjk.platform.ribbon.ServerFilter;
import com.google.common.util.concurrent.AtomicDouble;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

public class ZoneAvoidanceAndGrayAndLoadBasedRule extends ZoneAvoidanceRule {

    public static final Logger logger = LoggerFactory.getLogger(ZoneAvoidanceAndGrayAndLoadBasedRule.class);
    private double cpuWeight = 0.7;
    private double memoryWeight = 0.3;

    @Autowired(required = false)
    private List<ServerFilter> serverFilter;

    @Override
    public Server choose(Object key) {
        List<Server> reachableServerList = getLoadBalancer().getReachableServers();
        if (reachableServerList != null && reachableServerList.size() > 0) {
            List<Server> serverList = new ArrayList<>();
            serverList.addAll(reachableServerList);
            if (serverFilter != null) {
                for (ServerFilter f : serverFilter) {
                    serverList = f.match(serverList);
                }
            }
            if (serverList == null || serverList.size() == 0) {
                return null;
            }

            List<Server> eligibleServers = getPredicate().getEligibleServers(serverList, key);
            Server server = chooseInner(eligibleServers, key);
            if (server == null && serverList.size() > eligibleServers.size()) {
                server = chooseInner(serverList, key);
            }
            return server;
        } else {

            return null;
        }
    }

    protected Server chooseInner(List<Server> reachableServerList, Object key) {
        HashMap<Server, Double> serverScores = new HashMap<>();
        for (Server server : reachableServerList) {
            if (server instanceof DiscoveryEnabledServer) {
                DiscoveryEnabledServer discoveryEnabledServer = (DiscoveryEnabledServer)server;
                InstanceInfo serverInstanceInfo = discoveryEnabledServer.getInstanceInfo();
                Map<String, String> serverInstanceMetaData = serverInstanceInfo.getMetadata();
                String serverLoad = serverInstanceMetaData.get(Constants.EUREKA_METADATA_SERVERLOAD);
                if (serverLoad != null) {
                    ServerLoadStatus serverLoadStatus = JsonUtil.fromJson(serverLoad, ServerLoadStatus.class);
                    double cpuScore = 0;
                    if (serverLoadStatus.getAvailableProcessors() <= 0 | serverLoadStatus.getSystemLoadAverage() <= 0) {
                        cpuScore = 0.5;
                    } else {

                        cpuScore =
                            (serverLoadStatus.getAvailableProcessors() * 1.2 - serverLoadStatus.getSystemLoadAverage())
                                / (serverLoadStatus.getAvailableProcessors() * 1.2);
                    }
                    cpuScore = (cpuScore < 0) ? 0 : cpuScore;

                    double memoryScore = 0;
                    if (serverLoadStatus.getFreePhysicalMemorySize() <= 0
                        || serverLoadStatus.getTotalPhysicalMemorySize() <= 0) {
                        memoryScore = 0.5;
                    } else {
                        memoryScore = serverLoadStatus.getFreePhysicalMemorySize()
                            / serverLoadStatus.getTotalPhysicalMemorySize();
                    }
                    memoryScore = (memoryScore < 0) ? 0 : memoryScore;
                    serverScores.put(server, cpuScore * cpuWeight + memoryScore * memoryWeight);
                } else {

                    serverScores.put(server, 0.25 * cpuWeight + 0.25 * memoryWeight);
                }
            }
        }
        double totalScore = serverScores.values().stream().reduce(0.0, (total, r) -> total + r);

        HashMap<Server, Double[]> serverScoreRange = new HashMap<>();
        final AtomicDouble rangLeft = new AtomicDouble(0);
        serverScores.forEach((server, score) -> {
            double left = rangLeft.doubleValue();
            double right = rangLeft.addAndGet(score / totalScore);

            serverScoreRange.put(server, new Double[] {left, right});
        });
        double rollScore = Math.random();

        Server chooseServer = null;
        for (Map.Entry<Server, Double[]> entry : serverScoreRange.entrySet()) {
            Server server = entry.getKey();
            Double[] range = entry.getValue();
            if (rollScore >= range[0] && rollScore <= range[1]) {
                chooseServer = server;
                break;
            }
        }
        if (chooseServer == null && reachableServerList.size() > 0) {

            chooseServer = super.getPredicate().chooseRoundRobinAfterFiltering(reachableServerList, key).get();
        }
        return chooseServer;
    }

}
