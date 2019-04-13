
package com.bkjk.platform.eureka.support;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.util.Assert;

import com.bkjk.platform.common.Constants;
import com.bkjk.platform.common.ServerLoadStatus;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.netflix.appinfo.ApplicationInfoManager;

public class ScheduleReportServerLoad {

    private static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

        private final AtomicInteger mThreadNum = new AtomicInteger(1);

        private final String mPrefix;

        private final boolean mDaemon;

        private final ThreadGroup mGroup;

        public NamedThreadFactory() {
            this("TeslaScheduleCache-" + POOL_SEQ.getAndIncrement(), true);
        }

        public NamedThreadFactory(String prefix, boolean daemon) {
            mPrefix = prefix + "-thread-";
            mDaemon = daemon;
            SecurityManager s = System.getSecurityManager();
            mGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String name = mPrefix + mThreadNum.getAndIncrement();
            Thread ret = new Thread(mGroup, runnable, name, 0);
            ret.setDaemon(mDaemon);
            return ret;
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleReportServerLoad.class);

    final EurekaRegistration registration;

    public ScheduleReportServerLoad(EurekaRegistration registration) {
        this.registration = registration;
    }

    public void start() {
        ScheduledExecutorService scheduleReport = Executors.newScheduledThreadPool(1, new NamedThreadFactory());
        scheduleReport.scheduleAtFixedRate(new Runnable() {

            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                try {
                    Map<String, String> map = ApplicationInfoManager.getInstance().getInfo().getMetadata();
                    ServerLoadStatus serverLoadStatus = new ServerLoadStatus();
                    serverLoadStatus.calculateSystemInfo();
                    String serverLoadStatusJson = JsonUtil.toJson(serverLoadStatus);
                    map.put(Constants.EUREKA_METADATA_SERVERLOAD, serverLoadStatusJson);

                    Assert.notNull(registration, "registration MUST NOT BE NULL");
                    Assert.notNull(registration.getMetadata(), "registration.getMetadata() MUST NOT BE NULL");
                    Assert.notNull(ApplicationInfoManager.getInstance(),
                        "ApplicationInfoManager.getInstance() MUST NOT BE NULL");
                    Assert.notNull(ApplicationInfoManager.getInstance().getInfo(),
                        "ApplicationInfoManager.getInstance().getInfo() MUST NOT BE NULL");
                    Assert.notNull(ApplicationInfoManager.getInstance().getInfo().getMetadata(),
                        "ApplicationInfoManager.getInstance().getInfo().getMetadata() MUST NOT BE NULL");

                    map.putAll(registration.getMetadata());

                    ApplicationInfoManager.getInstance().registerAppMetadata(map);
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }

            }
        }, 0, 60, TimeUnit.SECONDS);
    }
}
