
package com.bkjk.platform.dts.server.remoting.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bkjk.platform.dts.common.api.DtsServerMessageHandler;
import com.bkjk.platform.dts.common.protocol.RequestCode;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.common.protocol.ResultCode;
import com.bkjk.platform.dts.common.protocol.header.BeginMessage;
import com.bkjk.platform.dts.common.protocol.header.BeginResultMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchRollBackMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchRollbackResultMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalRollbackMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalRollbackResultMessage;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchMessage;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchResultMessage;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.netty.NettyRequestProcessor;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;
import com.bkjk.platform.dts.remoting.protocol.RemotingSerializable;
import com.bkjk.platform.dts.remoting.protocol.RemotingSysResponseCode;

import io.netty.channel.ChannelHandlerContext;

@Component
@Qualifier("dtsMessageProcessor")
@Scope("prototype")
public class DtsMessageProcessor implements NettyRequestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DtsMessageProcessor.class);

    @Lookup
    protected DtsServerMessageHandler createMessageHandler() {
        return null;
    }

    private RemotingCommand processDtsMessage(String clientIp, RequestMessage dtsMessage) {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);
        CommandCustomHeader responseHeader;
        try {
            if (dtsMessage instanceof BeginMessage) {
                response = RemotingCommand.createResponseCommand(BeginResultMessage.class);
                responseHeader = response.readCustomHeader();
                createMessageHandler().handleMessage(clientIp, (BeginMessage)dtsMessage,
                    (BeginResultMessage)responseHeader);
                response.setCode(RemotingSysResponseCode.SUCCESS);
                return response;
            } else if (dtsMessage instanceof GlobalCommitMessage) {
                response = RemotingCommand.createResponseCommand(GlobalCommitResultMessage.class);
                responseHeader = response.readCustomHeader();
                createMessageHandler().handleMessage(clientIp, (GlobalCommitMessage)dtsMessage,
                    (GlobalCommitResultMessage)responseHeader);
                response.setCode(RemotingSysResponseCode.SUCCESS);
                return response;
            } else if (dtsMessage instanceof GlobalRollbackMessage) {
                response = RemotingCommand.createResponseCommand(GlobalRollbackResultMessage.class);
                responseHeader = response.readCustomHeader();
                createMessageHandler().handleMessage(clientIp, (GlobalRollbackMessage)dtsMessage,
                    (GlobalRollbackResultMessage)responseHeader);
                response.setCode(RemotingSysResponseCode.SUCCESS);
                return response;
            } else if (dtsMessage instanceof RegisterBranchMessage) {
                response = RemotingCommand.createResponseCommand(RegisterBranchResultMessage.class);
                responseHeader = response.readCustomHeader();
                createMessageHandler().handleMessage(clientIp, (RegisterBranchMessage)dtsMessage,
                    (RegisterBranchResultMessage)responseHeader);
                response.setCode(RemotingSysResponseCode.SUCCESS);
                return response;
            } else if (dtsMessage instanceof ClusterBranchCommitMessage) {
                response = RemotingCommand.createResponseCommand(ClusterBranchCommitResultMessage.class);
                responseHeader = response.readCustomHeader();
                ClusterBranchCommitResultMessage resultMessage = (ClusterBranchCommitResultMessage)responseHeader;
                createMessageHandler().handleMessage(clientIp, (ClusterBranchCommitMessage)dtsMessage, resultMessage);
                if (resultMessage.getBranchCommitResultMessageObj().getResult() == ResultCode.OK.getValue()) {
                    response.setCode(RemotingSysResponseCode.SUCCESS);
                } else {
                    response.setCode(RemotingSysResponseCode.SYSTEM_ERROR);
                    response.setRemark(resultMessage.getBranchCommitResultMessageObj().getReason());
                }
                return response;
            } else if (dtsMessage instanceof ClusterBranchRollBackMessage) {
                response = RemotingCommand.createResponseCommand(ClusterBranchRollbackResultMessage.class);
                responseHeader = response.readCustomHeader();
                ClusterBranchRollbackResultMessage resultMessage = (ClusterBranchRollbackResultMessage)responseHeader;
                createMessageHandler().handleMessage(clientIp, (ClusterBranchRollBackMessage)dtsMessage, resultMessage);
                if (resultMessage.getBranchRollbackResultMessageObj().getResult() == ResultCode.OK.getValue()) {
                    response.setCode(RemotingSysResponseCode.SUCCESS);
                } else {
                    response.setCode(RemotingSysResponseCode.SYSTEM_ERROR);
                    response.setRemark(resultMessage.getBranchRollbackResultMessageObj().getReason());
                }
                return response;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            response.setCode(RemotingSysResponseCode.SYSTEM_ERROR);
            response.setRemark(e.getMessage());
            return response;
        }
        response.setCode(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED);
        response.setRemark("not found request message proccessor");
        return response;
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        final String clientIp = NetWorkUtil.toStringAddress(ctx.channel().remoteAddress());
        switch (request.getCode()) {
            case RequestCode.HEADER_REQUEST:
                final RequestMessage headerMessage =
                    (RequestMessage)request.decodeCommandCustomHeader(CommandCustomHeader.class);
                return processDtsMessage(clientIp, headerMessage);
            case RequestCode.BODY_REQUEST:
                final byte[] body = request.getBody();
                RequestMessage bodyMessage = RemotingSerializable.decode(body, RequestMessage.class);
                return processDtsMessage(clientIp, bodyMessage);
            default:
                break;
        }
        final RemotingCommand response = RemotingCommand
            .createResponseCommand(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, "No request Code");
        return response;
    }

}
