
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class BeginMessage implements CommandCustomHeader, RequestMessage {

    @CFNotNull
    public long timeout = 60000;

    private String clientInfo;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public String getClientInfo() {
        return clientInfo;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
