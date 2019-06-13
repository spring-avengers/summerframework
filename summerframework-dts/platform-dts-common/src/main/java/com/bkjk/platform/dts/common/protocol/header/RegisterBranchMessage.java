
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class RegisterBranchMessage implements CommandCustomHeader, RequestMessage {

    @CFNotNull
    private long tranId;

    private String resourceInfo;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public String getResourceInfo() {
        return resourceInfo;
    }

    public long getTranId() {
        return tranId;
    }

    public void setResourceInfo(String resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

}
