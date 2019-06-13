package com.bkjk.platform.dts.common.protocol.header;

import com.alibaba.fastjson.JSON;
import com.bkjk.platform.dts.common.protocol.ResponseMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class ClusterBranchCommitResultMessage implements CommandCustomHeader, ResponseMessage {

    @CFNotNull
    private String branchCommitResultMessage;

    public BranchCommitResultMessage getBranchCommitResultMessageObj() {
        return JSON.parseObject(this.branchCommitResultMessage, BranchCommitResultMessage.class);
    }

    public String getBranchCommitResultMessage() {
        return branchCommitResultMessage;
    }

    public void setBranchCommitResultMessage(String branchCommitResultMessage) {
        this.branchCommitResultMessage = branchCommitResultMessage;
    }

    public void setBranchCommitResultMessage(BranchCommitResultMessage branchCommitResultMessage) {
        this.branchCommitResultMessage = JSON.toJSONString(branchCommitResultMessage);
    }

    @Override
    public void checkFields() throws RemotingCommandException {
        // TODO Auto-generated method stub

    }

}
