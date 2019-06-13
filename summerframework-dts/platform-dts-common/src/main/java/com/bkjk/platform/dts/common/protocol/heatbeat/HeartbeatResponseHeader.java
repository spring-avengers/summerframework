package com.bkjk.platform.dts.common.protocol.heatbeat;

import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class HeartbeatResponseHeader implements CommandCustomHeader, ResponseMessage {
    @Override
    public void checkFields() throws RemotingCommandException {

    }
}
