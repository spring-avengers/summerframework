package com.bkjk.platfrom.dts.core.resource;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.core.env.Environment;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.api.DtsClientMessageSender;
import com.bkjk.platform.dts.common.protocol.RequestCode;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.common.protocol.heatbeat.HeartbeatClientOrResourceInfo;
import com.bkjk.platform.dts.common.protocol.heatbeat.HeartbeatRequestHeader;
import com.bkjk.platform.dts.common.thread.DtsThreadFactory;
import com.bkjk.platform.dts.remoting.RemotingClient;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;
import com.bkjk.platform.dts.remoting.exception.RemotingConnectException;
import com.bkjk.platform.dts.remoting.exception.RemotingSendRequestException;
import com.bkjk.platform.dts.remoting.exception.RemotingTimeoutException;
import com.bkjk.platform.dts.remoting.netty.NettyClientConfig;
import com.bkjk.platform.dts.remoting.netty.NettyRemotingClient;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.bkjk.platfrom.dts.core.SpringContextHolder;
import com.bkjk.platfrom.dts.core.lb.DtsLoadbalance;
import com.google.common.collect.Queues;

public class DefaultDtsResourcMessageSender implements DtsClientMessageSender {
    private final RemotingClient remotingClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private final NettyClientConfig nettyClientConfig;
    private DtsLoadbalance dtsLB;
    private String clientInfo;

    public DefaultDtsResourcMessageSender() {
        this.nettyClientConfig = new NettyClientConfig();
        this.remotingClient = new NettyRemotingClient(nettyClientConfig);
        this.scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new DtsThreadFactory("ResourceHeadBetThread_"));
    }

    @Override
    public <T> T invoke(RequestMessage msg) throws DtsException {
        try {
            String server = dtsLB.chooseServer();
            RemotingCommand request = this.buildRequest(msg);
            RemotingCommand response =
                remotingClient.invokeSync(server, request, SpringContextHolder.getRpcInvokeTimeout());
            return this.buildResponse(response);
        } catch (RemotingCommandException | RemotingConnectException | RemotingSendRequestException
            | RemotingTimeoutException | InterruptedException e) {
            throw new DtsException(e);
        }
    }

    private void registerBodyRequest(DtsResourceManager reousrceManager) {
        ResourceMessageHandler messageProcessor = new ResourceMessageHandler(reousrceManager);
        BlockingQueue<Runnable> clientThreadPoolQueue = Queues.newLinkedBlockingDeque(100);
        ExecutorService clientMessageExecutor =
            new ThreadPoolExecutor(nettyClientConfig.getClientCallbackExecutorThreads(),
                nettyClientConfig.getClientCallbackExecutorThreads(), 1000 * 60, TimeUnit.MILLISECONDS,
                clientThreadPoolQueue, new DtsThreadFactory("ResourceBodyRequestThread_"));
        this.remotingClient.registerProcessor(RequestCode.BODY_REQUEST, messageProcessor, clientMessageExecutor);
    }

    private void registerHeaderRequest(DtsResourceManager reousrceManager) {
        ResourceMessageHandler messageProcessor = new ResourceMessageHandler(reousrceManager);
        BlockingQueue<Runnable> clientThreadPoolQueue = Queues.newLinkedBlockingDeque(100);
        ExecutorService clientMessageExecutor =
            new ThreadPoolExecutor(nettyClientConfig.getClientCallbackExecutorThreads(),
                nettyClientConfig.getClientCallbackExecutorThreads(), 1000 * 60, TimeUnit.MILLISECONDS,
                clientThreadPoolQueue, new DtsThreadFactory("ResourceHeadRequestThread_"));
        this.remotingClient.registerProcessor(RequestCode.HEADER_REQUEST, messageProcessor, clientMessageExecutor);
    }

    public void registerResourceManager(DtsResourceManager resourceManager) {
        registerHeaderRequest(resourceManager);
        registerBodyRequest(resourceManager);
    }

    public void start() {
        this.remotingClient.start();
        this.dtsLB = SpringContextHolder.getBean(DtsLoadbalance.class);
        HeartbeatClientOrResourceInfo client = new HeartbeatClientOrResourceInfo();
        client.setAppName(SpringContextHolder.getBean(Environment.class).getProperty("spring.application.name"));
        this.clientInfo = JsonUtil.toJson(client);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    HeartbeatRequestHeader hearbeat = new HeartbeatRequestHeader();
                    hearbeat.setClientOrResourceInfo(clientInfo);
                    DefaultDtsResourcMessageSender.this.invoke(hearbeat);
                } catch (Throwable e) {
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        this.remotingClient.shutdown();
        this.scheduledExecutorService.shutdownNow();
    }
}
