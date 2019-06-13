package com.bkjk.platform.dts.server.remoting.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.bkjk.platform.dts.common.protocol.RequestCode;
import com.bkjk.platform.dts.common.protocol.heatbeat.HeartbeatRequestHeader;
import com.bkjk.platform.dts.common.protocol.heatbeat.HeartbeatResponseHeader;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;
import com.bkjk.platform.dts.remoting.netty.NettyRequestProcessor;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;
import com.bkjk.platform.dts.remoting.protocol.RemotingSysResponseCode;
import com.bkjk.platform.dts.server.remoting.channel.ChannelInfo;
import com.bkjk.platform.dts.server.remoting.channel.ChannelRepository;

import io.netty.channel.ChannelHandlerContext;

@Component
@Qualifier("heatBeatProcessor")
public class HeatBeatProcessor implements NettyRequestProcessor {

    @Autowired
    private ChannelRepository channelRepository;

    private RemotingCommand heartbeat(ChannelHandlerContext ctx, RemotingCommand request)
        throws RemotingCommandException {
        HeartbeatRequestHeader header =
            (HeartbeatRequestHeader)request.decodeCommandCustomHeader(HeartbeatRequestHeader.class);
        ChannelInfo clientChannelInfo = new ChannelInfo(ctx.channel(), header.getClientOrResourceInfo());
        channelRepository.registerChannel(clientChannelInfo);
        RemotingCommand response = RemotingCommand.createResponseCommand(HeartbeatResponseHeader.class);
        response.setCode(RemotingSysResponseCode.SUCCESS);
        return response;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        switch (request.getCode()) {
            case RequestCode.HEART_BEAT:
                return this.heartbeat(ctx, request);
            default:
                break;
        }
        return null;
    }
}
