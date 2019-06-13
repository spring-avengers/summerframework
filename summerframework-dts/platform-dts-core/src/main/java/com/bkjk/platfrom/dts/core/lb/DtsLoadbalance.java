package com.bkjk.platfrom.dts.core.lb;

import com.bkjk.platform.dts.remoting.RemoteConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DtsLoadbalance {
    private static final Logger LOGGER = LoggerFactory.getLogger(DtsLoadbalance.class);
    private static final long INTERVAL = 5;
    private static final ScheduledExecutorService SCHEDULE_EXCUTOR = Executors.newScheduledThreadPool(1);
    private List<ServiceInstance> serviceInstanceList;
    private int index = 0;
    private int size = 0;

    public DtsLoadbalance(DiscoveryClient discoveryClient) {
        SCHEDULE_EXCUTOR.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    serviceInstanceList = discoveryClient.getInstances(RemoteConstant.DTS_SERVER_NAME);
                    size = serviceInstanceList.size();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }, 0, INTERVAL, TimeUnit.MINUTES);
    }

    public String chooseServer() {
        if (size == 0) {
            throw new NoSuchElementException("there is no dts server node discoveryed");
        }
        synchronized (this) {
            ServiceInstance instance = null;
            while(instance == null && serviceInstanceList.size() > 0) {
                index = index % serviceInstanceList.size();
                try {
                    instance = serviceInstanceList.get(index);
                }catch (IndexOutOfBoundsException e){
                    //ignore
                }
                index++;
            }
            return instance.getHost() + ":" + instance.getPort();
        }

    }
}
