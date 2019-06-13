package com.bkjk.platform.dts.common.protocol.heatbeat;

import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class HeartbeatRequestHeader implements CommandCustomHeader, RequestMessage {

    private String clientOrResourceInfo;

    @Override
    public void checkFields() throws RemotingCommandException {
    }

    public String getClientOrResourceInfo() {
        return clientOrResourceInfo;
    }

    public void setClientOrResourceInfo(String dbName) {
        this.clientOrResourceInfo = dbName;
    }

}
