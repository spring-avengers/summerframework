
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class BranchRollBackMessage implements CommandCustomHeader, RequestMessage, ResourceInfoMessage {

    @CFNotNull
    private long tranId;

    @CFNotNull
    private long branchId;

    @CFNotNull
    private String resourceIp;

    @CFNotNull
    private String resourceInfo;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public long getBranchId() {
        return branchId;
    }

    @Override
    public String getResourceInfo() {
        return resourceInfo;
    }

    @Override
    public String getResourceIp() {
        return resourceIp;
    }

    public long getTranId() {
        return tranId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public void setResourceInfo(String resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public void setResourceIp(String resourceIp) {
        this.resourceIp = resourceIp;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

}
