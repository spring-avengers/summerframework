
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class BeginResultMessage implements CommandCustomHeader, ResponseMessage {

    @CFNotNull
    private String xid;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

}
