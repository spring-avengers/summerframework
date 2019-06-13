package com.bkjk.platform.dts.common.protocol.header;

import com.alibaba.fastjson.JSON;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class ClusterBranchRollbackResultMessage implements CommandCustomHeader, RequestMessage {

    @CFNotNull
    private String branchRollbackResultMessage;

    public String getBranchRollbackResultMessage() {
        return branchRollbackResultMessage;
    }

    public BranchRollbackResultMessage getBranchRollbackResultMessageObj() {
        return JSON.parseObject(this.branchRollbackResultMessage, BranchRollbackResultMessage.class);
    }

    public void setBranchRollbackResultMessage(String branchRollbackResultMessage) {
        this.branchRollbackResultMessage = branchRollbackResultMessage;
    }

    public void setBranchRollbackResultMessage(BranchRollbackResultMessage branchRollbackResultMessage) {
        this.branchRollbackResultMessage = JSON.toJSONString(branchRollbackResultMessage);
    }

    @Override
    public void checkFields() throws RemotingCommandException {

    }

}
