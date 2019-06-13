
package com.bkjk.platform.dts.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.api.DtsServerMessageSender;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchMessage;
import com.bkjk.platform.dts.server.model.BranchLog;
import com.bkjk.platform.dts.server.model.BranchLogState;
import com.bkjk.platform.dts.server.model.GlobalLog;
import com.bkjk.platform.dts.server.model.GlobalLogState;
import com.bkjk.platform.dts.server.storage.TransactionLogStorage;

public interface ResouceMessageHandler {

    public static ResouceMessageHandler createResouceMessageHandler(TransactionLogStorage dtsLogDao,
        DtsServerMessageSender messageSender) {

        return new ResouceMessageHandler() {
            private final Logger logger = LoggerFactory.getLogger(ResouceMessageHandler.class);

            @Override
            public Long processMessage(RegisterBranchMessage registerMessage, String clientIp) {
                long tranId = registerMessage.getTranId();
                GlobalLog globalLog = dtsLogDao.getGlobalLog(tranId);
                if (globalLog == null || globalLog.getState() != GlobalLogState.Begin.getValue()) {
                    if (globalLog == null) {
                        throw new DtsException("Transaction " + tranId + " doesn't exist");
                    } else {
                        throw new DtsException("Transaction " + tranId + " is in state:" + globalLog.getState());
                    }
                }
                BranchLog branchLog = new BranchLog();
                branchLog.setTransId(tranId);
                branchLog.setResourceInfo(registerMessage.getResourceInfo());
                branchLog.setResourceIp(clientIp);
                branchLog.setState(BranchLogState.Begin.getValue());
                dtsLogDao.insertBranchLog(branchLog);
                Long branchId = branchLog.getBranchId();
                globalLog.getBranchIds().add(branchId);
                logger.info("Created branch log[TransId:{},BranchId:{},ResourceId:{},ResourceInfo:{}]", tranId,
                    branchId, clientIp, registerMessage.getResourceInfo());
                return branchId;
            }

        };
    }

    Long processMessage(RegisterBranchMessage registerMessage, String clientIp);

}
