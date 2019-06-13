
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class RegisterBranchResultMessage implements CommandCustomHeader, ResponseMessage {

    @CFNotNull
    private long tranId;

    @CFNotNull
    private long branchId;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public long getBranchId() {
        return branchId;
    }

    public long getTranId() {
        return tranId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

}
