package com.bkjk.platfrom.dts.core.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platform.dts.common.protocol.RequestCode;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.common.protocol.ResultCode;
import com.bkjk.platform.dts.common.protocol.header.BranchCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchRollBackMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchRollbackResultMessage;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.netty.NettyRequestProcessor;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;
import com.bkjk.platform.dts.remoting.protocol.RemotingSerializable;
import com.bkjk.platform.dts.remoting.protocol.RemotingSysResponseCode;

import io.netty.channel.ChannelHandlerContext;

public class ResourceMessageHandler implements NettyRequestProcessor {

    private final DtsResourceManager dtsResourceManager;
    Logger logger = LoggerFactory.getLogger(ResourceMessageHandler.class);

    public ResourceMessageHandler(DtsResourceManager dtsResourceManager) {
        this.dtsResourceManager = dtsResourceManager;
    }

    private void handleMessage(final String serverAddressIp, final BranchCommitMessage commitMessage,
        final BranchCommitResultMessage resultMessage) {
        logger.info(
            "Handle BranchCommitMessage[TranId:{},BranchId:{},ResourceInfo:{},ResourceIp:{},ServerAddressIp:{}]",
            commitMessage.getTranId(), commitMessage.getBranchId(), commitMessage.getResourceInfo(),
            commitMessage.getResourceIp(), serverAddressIp);
        Long branchId = commitMessage.getBranchId();
        Long tranId = commitMessage.getTranId();
        resultMessage.setBranchId(branchId);
        resultMessage.setTranId(tranId);
        try {
            dtsResourceManager.branchCommit(tranId, branchId);
            resultMessage.setResult(ResultCode.OK.getValue());
        } catch (Exception e) {
            resultMessage.setReason(e.getMessage());
            resultMessage.setResult(ResultCode.ERROR.getValue());
        }
    }

    private void handleMessage(final String serverAddressIP, final BranchRollBackMessage rollBackMessage,
        final BranchRollbackResultMessage resultMessage) {
        logger.info(
            "Handle BranchRollBackMessage[TranId:{},BranchId:{},ResourceInfo:{},ResourceIp:{},ServerAddressIp:{}]",
            rollBackMessage.getTranId(), rollBackMessage.getBranchId(), rollBackMessage.getResourceInfo(),
            rollBackMessage.getResourceIp(), serverAddressIP);
        Long branchId = rollBackMessage.getBranchId();
        Long tranId = rollBackMessage.getTranId();
        resultMessage.setBranchId(branchId);
        resultMessage.setTranId(tranId);
        try {
            dtsResourceManager.branchRollback(tranId, branchId);
            resultMessage.setResult(ResultCode.OK.getValue());
        } catch (Exception e) {
            resultMessage.setReason(e.getMessage());
            resultMessage.setResult(ResultCode.ERROR.getValue());
        }
    }

    private RemotingCommand processDtsMessage(String serverAddressIp, RequestMessage dtsMessage) {
        RemotingCommand response = RemotingCommand.createResponseCommand(null);
        CommandCustomHeader responseHeader;
        try {
            if (dtsMessage instanceof BranchCommitMessage) {

                response = RemotingCommand.createResponseCommand(BranchCommitResultMessage.class);
                responseHeader = response.readCustomHeader();
                handleMessage(serverAddressIp, (BranchCommitMessage)dtsMessage,
                    (BranchCommitResultMessage)responseHeader);
                response.setCode(RemotingSysResponseCode.SUCCESS);
                return response;
            } else if (dtsMessage instanceof BranchRollBackMessage) {

                response = RemotingCommand.createResponseCommand(BranchRollbackResultMessage.class);
                responseHeader = response.readCustomHeader();
                handleMessage(serverAddressIp, (BranchRollBackMessage)dtsMessage,
                    (BranchRollbackResultMessage)responseHeader);
                response.setCode(RemotingSysResponseCode.SUCCESS);
                return response;
            }
        } catch (Throwable e) {
            response.setCode(RemotingSysResponseCode.SYSTEM_ERROR);
            response.setRemark(e.getMessage());
            return response;
        }
        response.setCode(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED);
        response.setRemark("not found request message proccessor");
        return response;
    }

    @Override
    public RemotingCommand processRequest(final ChannelHandlerContext ctx, final RemotingCommand request)
        throws Exception {
        final String serverAddressIp = NetWorkUtil.toStringAddress(ctx.channel().remoteAddress());
        switch (request.getCode()) {
            case RequestCode.HEADER_REQUEST:
                final RequestMessage headerMessage =
                    (RequestMessage)request.decodeCommandCustomHeader(CommandCustomHeader.class);
                return processDtsMessage(serverAddressIp, headerMessage);
            case RequestCode.BODY_REQUEST:
                final byte[] body = request.getBody();
                RequestMessage bodyMessage = RemotingSerializable.decode(body, RequestMessage.class);
                return processDtsMessage(serverAddressIp, bodyMessage);
            default:
                break;
        }
        final RemotingCommand response = RemotingCommand
            .createResponseCommand(RemotingSysResponseCode.REQUEST_CODE_NOT_SUPPORTED, "No request Code");
        return response;
    }

}
