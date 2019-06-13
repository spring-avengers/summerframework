package com.bkjk.platform.dts.common.api;

import com.bkjk.platform.dts.common.protocol.header.BeginMessage;
import com.bkjk.platform.dts.common.protocol.header.BeginResultMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchRollBackMessage;
import com.bkjk.platform.dts.common.protocol.header.ClusterBranchRollbackResultMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalRollbackMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalRollbackResultMessage;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchMessage;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchResultMessage;

public interface DtsServerMessageHandler {

    public void handleMessage(String clientIp, BeginMessage message, BeginResultMessage resultMessage);

    public void handleMessage(String clientIp, GlobalCommitMessage message, GlobalCommitResultMessage resultMessage);

    public void handleMessage(String clientIp, GlobalRollbackMessage message,
        GlobalRollbackResultMessage resultMessage);

    public void handleMessage(String clientIp, RegisterBranchMessage message,
        RegisterBranchResultMessage resultMessage);

    public void handleMessage(String clientIp, ClusterBranchCommitMessage message,
        ClusterBranchCommitResultMessage resultMessage);

    public void handleMessage(String clientIp, ClusterBranchRollBackMessage message,
        ClusterBranchRollbackResultMessage resultMessage);

}
