package com.bkjk.platform.dts.common.protocol.header;

import com.alibaba.fastjson.JSON;
import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.remoting.CommandCustomHeader;
import com.bkjk.platform.dts.remoting.annotation.CFNotNull;
import com.bkjk.platform.dts.remoting.exception.RemotingCommandException;

public class ClusterBranchCommitMessage implements CommandCustomHeader, RequestMessage {

    @CFNotNull
    private String branchCommitMessage;

    public String getBranchCommitMessage() {
        return branchCommitMessage;
    }

    public void setBranchCommitMessage(BranchCommitMessage branchCommitMessage) {
        this.branchCommitMessage = JSON.toJSONString(branchCommitMessage);
    }

    public void setBranchCommitMessage(String branchCommitMessage) {
        this.branchCommitMessage = branchCommitMessage;
    }

    public BranchCommitMessage getBranchCommitMessageObj() {
        return JSON.parseObject(this.branchCommitMessage, BranchCommitMessage.class);
    }

    @Override
    public void checkFields() throws RemotingCommandException {

    }

}
