package com.bkjk.platfrom.dts.core.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.core.env.Environment;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.api.DtsClientMessageSender;
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

public class DefaultDtsClientMessageSender implements DtsClientMessageSender {
    private final RemotingClient remotingClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private DtsLoadbalance dtsLB;
    private String clientInfo;

    public DefaultDtsClientMessageSender() {
        final NettyClientConfig nettyClientConfig = new NettyClientConfig();
        this.remotingClient = new NettyRemotingClient(nettyClientConfig);
        this.scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new DtsThreadFactory("ClientHeadBetThread_"));
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

    public void start() {
        this.remotingClient.start();
        this.dtsLB = SpringContextHolder.getBean(DtsLoadbalance.class);
        HeartbeatClientOrResourceInfo client = new HeartbeatClientOrResourceInfo();
        client.setAppName(SpringContextHolder.getBean(Environment.class).getProperty("spring.application.name"));
        client.setFromClient(true);
        this.clientInfo = JsonUtil.toJson(client);
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    HeartbeatRequestHeader hearbeat = new HeartbeatRequestHeader();
                    hearbeat.setClientOrResourceInfo(clientInfo);
                    DefaultDtsClientMessageSender.this.invoke(hearbeat);
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
