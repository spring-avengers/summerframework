
package com.bkjk.platform.dts.server.handler;

import com.bkjk.platform.dts.common.DtsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platform.dts.common.api.DtsServerMessageSender;
import com.bkjk.platform.dts.common.protocol.ResultCode;
import com.bkjk.platform.dts.common.protocol.header.BranchCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchRollbackResultMessage;
import com.bkjk.platform.dts.server.model.BranchLog;
import com.bkjk.platform.dts.server.model.BranchLogState;
import com.bkjk.platform.dts.server.storage.TransactionLogStorage;

public interface NotifyResultMessagHandler {

    public static NotifyResultMessagHandler createNotifyResultMessagHandler(final TransactionLogStorage dtsLogDao,
        final DtsServerMessageSender messageSender) {

        return new NotifyResultMessagHandler() {

            private final Logger logger = LoggerFactory.getLogger(NotifyResultMessagHandler.class);

            @Override
            public void processMessage(String clientIp, BranchCommitResultMessage message) {
                Long branchId = message.getBranchId();
                if (message.getResult() == ResultCode.OK.getValue()) {
                    logger.info(
                        "Notify branch[branchId:{},clientIp:{}] of global transaction[{}] commit succeed, clearing branch log",
                        message.getBranchId(), clientIp, message.getTranId());
                    dtsLogDao.deleteBranchLog(branchId, BranchLogState.Success.getValue());
                } else if (message.getResult() == ResultCode.ERROR.getValue()) {
                    logger.error(
                        "Notify branch[branchId:{},clientIp:{}] of global transaction[{}] commit failed by reason[{}], updating this branch log to failed status and creating newly error branch log",
                        message.getBranchId(), clientIp, message.getTranId(), message.getReason());
                    synchronized (messageSender) {
                        BranchLog branchLog = dtsLogDao.getBranchLog(branchId);
                        branchLog.setState(BranchLogState.Failed.getValue());
                        dtsLogDao.updateBranchLog(branchLog);
                        branchLog.setResourceInfo(branchLog.getResourceInfo() + ":" + message.getReason());
                        dtsLogDao.insertBranchErrorLog(branchLog);
                    }
                    logger.error("Logic error occurs while commit branch:" + branchId
                        + ". Please check server table:dts_branch_error_log");
                    throw new DtsException(
                        "Notify branch[branchId:{},clientIp:{}] of global transaction[{}] commit failed");
                }
            }

            @Override
            public void processMessage(String clientIp, BranchRollbackResultMessage message) {
                Long branchId = message.getBranchId();
                if (message.getResult() == ResultCode.OK.getValue()) {
                    logger.info(
                        "Notify branch[branchId:{},clientIp:{}] of global transaction[{}] rollback succeed, clearing branch log",
                        message.getBranchId(), clientIp, message.getTranId());
                    dtsLogDao.deleteBranchLog(branchId, BranchLogState.Success.getValue());
                } else if (message.getResult() == ResultCode.ERROR.getValue()) {
                    logger.error(
                        "Notify branch[branchId:{},clientIp:{}] of global transaction[{}] rollback failed by reason[{}], updating this branch log to failed status and creating newly error branch log",
                        message.getBranchId(), clientIp, message.getTranId(), message.getReason());
                    synchronized (messageSender) {
                        BranchLog branchLog = dtsLogDao.getBranchLog(branchId);
                        branchLog.setState(BranchLogState.Failed.getValue());
                        dtsLogDao.updateBranchLog(branchLog);
                        branchLog.setResourceInfo(branchLog.getResourceInfo() + ":" + message.getReason());
                        dtsLogDao.insertBranchErrorLog(branchLog);
                    }
                    logger.error("Logic error occurs while rollback branch:" + message.getBranchId()
                        + ". Please check server table:dts_branch_error_log");
                    throw new DtsException(
                        "Notify branch[branchId:{},clientIp:{}] of global transaction[{}] rollback failed");
                }
            }
        };
    }

    void processMessage(String clientIp, BranchCommitResultMessage message);

    void processMessage(String clientIp, BranchRollbackResultMessage message);
}
