package com.bkjk.platform.dts.common.protocol.header;

import com.alibaba.fastjson.JSON;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class ClusterBranchRollBackMessage implements CommandCustomHeader, RequestMessage {

    @CFNotNull
    private String branchRollBackMessage;

    public BranchRollBackMessage getBranchRollBackMessageObj() {
        return JSON.parseObject(this.branchRollBackMessage, BranchRollBackMessage.class);
    }

    public String getBranchRollBackMessage() {
        return branchRollBackMessage;
    }

    public void setBranchRollBackMessage(BranchRollBackMessage branchRollBackMessage) {
        this.branchRollBackMessage = JSON.toJSONString(branchRollBackMessage);
    }

    public void setBranchRollBackMessage(String branchRollBackMessage) {
        this.branchRollBackMessage = branchRollBackMessage;
    }

    @Override
    public void checkFields() throws RemotingCommandException {

    }

}
