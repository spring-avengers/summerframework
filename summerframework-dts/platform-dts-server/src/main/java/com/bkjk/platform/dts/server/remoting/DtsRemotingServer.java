
package com.bkjk.platform.dts.server.remoting;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bkjk.platform.dts.common.DtsXID;
import com.bkjk.platform.dts.common.protocol.RequestCode;
import com.bkjk.platform.dts.common.thread.DtsThreadFactory;
import com.bkjk.platform.dts.common.thread.ServerFixedThreadPoolExecutor;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platform.dts.remoting.RemotingServer;
import com.bkjk.platform.dts.remoting.netty.NettyRemotingServer;
import com.bkjk.platform.dts.remoting.netty.NettyServerConfig;
import com.bkjk.platform.dts.server.remoting.channel.ChannelkeepingComponent;
import com.bkjk.platform.dts.server.remoting.processor.DtsMessageProcessor;
import com.bkjk.platform.dts.server.remoting.processor.HeatBeatProcessor;
import com.google.common.collect.Queues;

@Component
public class DtsRemotingServer {

    private static final Integer cpus = Runtime.getRuntime().availableProcessors();

    @Autowired
    private ChannelkeepingComponent channelKeeping;

    @Autowired
    @Qualifier("heatBeatProcessor")
    private HeatBeatProcessor heatBeatProccessor;

    @Value("${server.port}")
    private int port;

    @Value("${dts.headerRequest.corePoolSizeCpuTimes:30}")
    private int headerRequestCorePoolSizeCpuTimes;

    @Value("${dts.headerRequest.maximumPoolSizeCpuTimes:30}")
    private int headerRequestMaximumPoolSizeCpuTimes;

    @Value("${dts.headerRequest.headerRequestKeepaliveTime:60000}")
    private int headerRequestKeepaliveTime;

    private RemotingServer remotingServer;

    @Lookup(value = "dtsMessageProcessor")
    protected DtsMessageProcessor createMessageProcessor() {
        return null;
    }

    public RemotingServer getRemotingServer() {
        return remotingServer;
    }

    @PostConstruct
    public void init() {
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(port);
        this.remotingServer = new NettyRemotingServer(nettyServerConfig, channelKeeping);
        this.registerProcessor();
        DtsXID.setIpAddress(NetWorkUtil.getLocalIp());
        DtsXID.setPort(port);
    }

    private void registerBodyRequest() {
        DtsMessageProcessor messageProcessor = createMessageProcessor();
        BlockingQueue<Runnable> resourceThreadPoolQueue = Queues.newLinkedBlockingDeque(10000);
        ExecutorService resourceMessageExecutor = new ServerFixedThreadPoolExecutor(cpus, cpus, 1000 * 60,
            TimeUnit.MILLISECONDS, resourceThreadPoolQueue, new DtsThreadFactory("ServerBodyThread_"));
        this.remotingServer.registerProcessor(RequestCode.BODY_REQUEST, messageProcessor, resourceMessageExecutor);
    }

    private void registerHeaderRequest() {
        DtsMessageProcessor messageProcessor = createMessageProcessor();
        BlockingQueue<Runnable> clientThreadPoolQueue = Queues.newLinkedBlockingDeque(10000);
        ExecutorService clientMessageExecutor =
            new ServerFixedThreadPoolExecutor(cpus * headerRequestCorePoolSizeCpuTimes,
                cpus * headerRequestMaximumPoolSizeCpuTimes, headerRequestKeepaliveTime, TimeUnit.MILLISECONDS,
                clientThreadPoolQueue, new DtsThreadFactory("ServerHeadRequestThread_"));
        this.remotingServer.registerProcessor(RequestCode.HEADER_REQUEST, messageProcessor, clientMessageExecutor);
    }

    private void registerHeatBeatRequest() {
        ExecutorService heatBeatProcessorExecutor =
            Executors.newFixedThreadPool(cpus, new DtsThreadFactory("ServerHeadBeatThread_"));
        this.remotingServer.registerProcessor(RequestCode.HEART_BEAT, heatBeatProccessor, heatBeatProcessorExecutor);
    }

    private void registerProcessor() {
        registerHeaderRequest();
        registerBodyRequest();
        registerHeatBeatRequest();
    }

    public void start() {
        if (this.remotingServer != null) {
            this.remotingServer.start();
        }
        if (this.channelKeeping != null) {
            this.channelKeeping.start();
        }
    }

    @PreDestroy
    public void stop() {
        if (this.channelKeeping != null) {
            this.channelKeeping.stop();
        }
        if (this.remotingServer != null) {
            this.remotingServer.shutdown();
        }
    }
}
