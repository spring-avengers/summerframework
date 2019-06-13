
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class BranchRollbackResultMessage implements CommandCustomHeader, ResponseMessage {

    @CFNotNull
    private long tranId;

    @CFNotNull
    private long branchId;

    @CFNotNull
    private int result;

    @CFNotNull
    private String reason;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public long getBranchId() {
        return branchId;
    }

    public String getReason() {
        return reason;
    }

    public int getResult() {
        return result;
    }

    public long getTranId() {
        return tranId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

}
