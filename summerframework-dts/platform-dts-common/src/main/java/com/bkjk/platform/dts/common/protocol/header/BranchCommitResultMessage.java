
package com.bkjk.platform.dts.common.protocol.header;

import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class BranchCommitResultMessage implements CommandCustomHeader, ResponseMessage {

    @CFNotNull
    private Long tranId;

    @CFNotNull
    private Long branchId;

    @CFNotNull
    private int result;

    @CFNotNull
    private String reason;

    @Override
    public void checkFields() throws RemotingCommandException {

    }

    public Long getBranchId() {
        return branchId;
    }

    public String getReason() {
        return reason;
    }

    public int getResult() {
        return result;
    }

    public Long getTranId() {
        return tranId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public void setTranId(Long tranId) {
        this.tranId = tranId;
    }

}
