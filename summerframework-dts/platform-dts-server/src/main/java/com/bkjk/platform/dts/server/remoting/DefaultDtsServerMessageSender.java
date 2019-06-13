
package com.bkjk.platform.dts.server.remoting;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.api.BaseMessageSender;
import com.bkjk.platform.dts.common.api.DtsServerMessageSender;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.common.protocol.header.*;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platform.dts.remoting.RemoteConstant;
import com.bkjk.platform.dts.remoting.RemotingClient;
import com.bkjk.platform.dts.remoting.exception.*;
import com.bkjk.platform.dts.remoting.netty.NettyClientConfig;
import com.bkjk.platform.dts.remoting.netty.NettyRemotingClient;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;
import com.bkjk.platform.dts.server.remoting.channel.ChannelRepository;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Component
public class DefaultDtsServerMessageSender implements DtsServerMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDtsServerMessageSender.class);

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private DtsRemotingServer dtsServerContainer;

    @Value("${dts.rpcInvokeTimeout:30000}")
    private long rpcInvokeTimeout;

    @Autowired
    private DiscoveryClient discoveryClient;

    private DtsClusterMessageSender clusterMessageSender;

    @PostConstruct
    public void init() {
        clusterMessageSender = new DtsClusterMessageSender();
        clusterMessageSender.start();
    }

    @PreDestroy
    public void destroy() {
        if (clusterMessageSender != null)
            clusterMessageSender.stop();
    }

    @Override
    public void invokeAsync(String clientAddress, RequestMessage msg) throws DtsException {
        Channel channel = channelRepository.getChannelByAddress(clientAddress, msg);
        if (channel != null) {
            RemotingCommand request = this.buildRequest(msg);
            try {
                dtsServerContainer.getRemotingServer().invokeAsync(channel, request, rpcInvokeTimeout, null);
            } catch (RemotingSendRequestException | RemotingTimeoutException | InterruptedException
                | RemotingTooMuchRequestException e) {
                throw new DtsException(e);
            }
        } else {
            this.invokeClusterSync(clientAddress, msg);
        }
    }

    @Override
    public <T> T invokeSync(String clientAddress, RequestMessage msg) throws DtsException {
        Channel channel = channelRepository.getChannelByAddress(clientAddress, msg);
        if (channel != null) {
            RemotingCommand request = this.buildRequest(msg);
            try {
                RemotingCommand response =
                    dtsServerContainer.getRemotingServer().invokeSync(channel, request, rpcInvokeTimeout);
                return this.buildResponse(response);
            } catch (RemotingSendRequestException | RemotingTimeoutException | InterruptedException
                | RemotingCommandException e) {
                throw new DtsException(e);
            }
        } else {
            return this.invokeClusterSync(clientAddress, msg);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeClusterSync(String clientAddress, RequestMessage msg) throws DtsException {
        if (msg instanceof BranchCommitMessage) {
            ClusterBranchCommitMessage clusterBranchCommitMessage = new ClusterBranchCommitMessage();
            clusterBranchCommitMessage.setBranchCommitMessage((BranchCommitMessage)msg);
            ClusterBranchCommitResultMessage clusterBranchCommitResultMessage =
                this.clusterCycleSync(clusterBranchCommitMessage);
            return (T)clusterBranchCommitResultMessage.getBranchCommitResultMessageObj();
        }
        if (msg instanceof BranchRollBackMessage) {
            ClusterBranchRollBackMessage clusterBranchRollBackMessage = new ClusterBranchRollBackMessage();
            clusterBranchRollBackMessage.setBranchRollBackMessage((BranchRollBackMessage)msg);
            ClusterBranchRollbackResultMessage clusterBranchRollbackResultMessage =
                this.clusterCycleSync(clusterBranchRollBackMessage);
            return (T)clusterBranchRollbackResultMessage.getBranchRollbackResultMessageObj();
        }
        return null;
    }

    private <T> T clusterCycleSync(RequestMessage msg) throws DtsException {
        List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(RemoteConstant.DTS_SERVER_NAME);
        for (int i = 0; i < serviceInstanceList.size(); i++) {
            ServiceInstance instance = serviceInstanceList.get(i);
            if (NetWorkUtil.getLocalIp().equals(instance.getHost())
                && instance.getPort() == dtsServerContainer.getRemotingServer().localListenPort()) {
                continue;
            }
            String server = instance.getHost() + ":" + instance.getPort();
            try {
                return clusterMessageSender.invoke(server, msg);
            } catch (Throwable e) {
                logger.warn("cluster sync {} send {} failed! will try next server", server, msg);
            }
        }
        throw new DtsException("No available server for clusterCycleSync");
    }

    private static class DtsClusterMessageSender implements BaseMessageSender {
        private final RemotingClient remotingClient;

        public DtsClusterMessageSender() {
            final NettyClientConfig nettyClientConfig = new NettyClientConfig();
            this.remotingClient = new NettyRemotingClient(nettyClientConfig);
        }

        public <T> T invoke(String server, RequestMessage msg) throws DtsException {
            try {
                RemotingCommand request = this.buildRequest(msg);
                RemotingCommand response =
                    remotingClient.invokeSync(server, request, RemoteConstant.RPC_INVOKE_TIMEOUT);
                return this.buildResponse(response);
            } catch (RemotingCommandException | RemotingConnectException | RemotingSendRequestException
                | RemotingTimeoutException | InterruptedException e) {
                throw new DtsException(e);
            }
        }

        public void start() {
            this.remotingClient.start();
        }

        public void stop() {
            this.remotingClient.shutdown();
        }

    }

}
