package com.bkjk.platform.ribbon.wrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.bkjk.platform.common.Constants;
import com.bkjk.platform.common.ServerLoadStatus;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

public class TestZoneAvoidanceRuleWrapper {
    private static DiscoveryEnabledServer createDiscoveryEnabledServer(double load) {

        Map<String, String> metadata = new HashMap<>();
        ServerLoadStatus serverLoad = new ServerLoadStatus();
        serverLoad.calculateSystemInfo();
        serverLoad.setSystemLoadAverage(load);
        serverLoad.setOsName(serverLoad.getOsName() + "-" + load);
        String serverLoadJson = JsonUtil.toJson(serverLoad);
        metadata.put(Constants.EUREKA_METADATA_SERVERLOAD, serverLoadJson);
        InstanceInfo instanceInfo1 = InstanceInfo.Builder.newBuilder().setHostName("host" + load)
            .setAppName("app" + load).setMetadata(metadata).build();
        DiscoveryEnabledServer server = new DiscoveryEnabledServer(instanceInfo1, false);
        return server;
    }

    public static void main(String[] args) {
        ZoneAvoidanceAndGrayAndLoadBasedRule zoneAvoidanceRuleWrapper = new ZoneAvoidanceAndGrayAndLoadBasedRule();
        List<Server> servers = new ArrayList<>();
        servers.add(createDiscoveryEnabledServer(4));
        servers.add(createDiscoveryEnabledServer(8));
        servers.add(createDiscoveryEnabledServer(7));

        HashMap<Server, AtomicInteger> count = new HashMap<>();
        long start = System.nanoTime();
        int c = 1000;
        for (int i = 0; i < c; i++) {
            Server s = zoneAvoidanceRuleWrapper.chooseInner(servers, null);
            count.put(s, count.getOrDefault(s, new AtomicInteger(0)));
            count.get(s).incrementAndGet();
        }
        System.out.println("Used:" + (System.nanoTime() - start) / 1000_000 + "ms");
        System.out.println(count);
    }
}
