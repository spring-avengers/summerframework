
package com.bkjk.platform.dts.common.api;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.protocol.RequestCode;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.common.protocol.heatbeat.HeartbeatRequestHeader;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;
import com.bkjk.platform.dts.remoting.protocol.RemotingCommand;
import com.bkjk.platform.dts.remoting.protocol.RemotingSerializable;
import com.bkjk.platform.dts.remoting.protocol.RemotingSysResponseCode;

@SuppressWarnings("unchecked")
public interface BaseMessageSender {
    default RemotingCommand buildRequest(RequestMessage dtsMessage) throws DtsException {
        RemotingCommand request = null;
        if (dtsMessage instanceof CommandCustomHeader) {
            if (dtsMessage instanceof HeartbeatRequestHeader) {
                request = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, (CommandCustomHeader)dtsMessage);
            } else {
                request =
                    RemotingCommand.createRequestCommand(RequestCode.HEADER_REQUEST, (CommandCustomHeader)dtsMessage);
            }
        } else if (dtsMessage instanceof RemotingSerializable) {
            request = RemotingCommand.createRequestCommand(RequestCode.BODY_REQUEST, null);
            request.setBody(RemotingSerializable.encode1(dtsMessage));
        } else {
            throw new DtsException("request must implements CommandCustomHeader or RemotingSerializable");
        }
        return request;
    }

    default <T> T buildResponse(RemotingCommand response) throws RemotingCommandException {
        if (response.getCode() == RemotingSysResponseCode.SUCCESS) {
            if (!response.getExtFields().isEmpty()) {
                return (T)response.decodeCommandCustomHeader(CommandCustomHeader.class);
            } else if (response.getBody() != null) {
                return (T)RemotingSerializable.decode(response.getBody(), ResponseMessage.class);
            }
        } else {
            throw new DtsException(response.getRemark());
        }
        return null;
    }
}
