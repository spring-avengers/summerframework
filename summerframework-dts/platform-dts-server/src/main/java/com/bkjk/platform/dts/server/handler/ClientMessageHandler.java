
package com.bkjk.platform.dts.server.handler;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.DtsXID;
import com.bkjk.platform.dts.common.api.DtsServerMessageSender;
import com.bkjk.platform.dts.common.protocol.header.*;
import com.bkjk.platform.dts.server.model.BranchLog;
import com.bkjk.platform.dts.server.model.BranchLogState;
import com.bkjk.platform.dts.server.model.GlobalLog;
import com.bkjk.platform.dts.server.model.GlobalLogState;
import com.bkjk.platform.dts.server.storage.TransactionLogStorage;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.EntryExpiredListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public interface ClientMessageHandler extends EntryExpiredListener<Long, GlobalLog> {

    public static ClientMessageHandler createClientMessageHandler(TransactionLogStorage logStorage,
        DtsServerMessageSender messageSender, IMap<Long, GlobalLog> commitLogCache,
        IMap<Long, GlobalLog> rollbackLogCache) {

        return new ClientMessageHandler() {
            private final Logger logger = LoggerFactory.getLogger(ResouceMessageHandler.class);
            private final NotifyResultMessagHandler globalResultMessageHandler =
                NotifyResultMessagHandler.createNotifyResultMessagHandler(logStorage, messageSender);

            @Override
            public void entryExpired(EntryEvent<Long, GlobalLog> event) {
                long tranId = event.getKey();
                logger.error("Global transaction[transId:{},state:{}] expired, notify rollback anyway", tranId,
                    GlobalLogState.parse(event.getValue().getState()).name());
                GlobalLog globalLog = logStorage.getGlobalLog(tranId);
                List<BranchLog> branchLogs = logStorage.getBranchLogs(tranId);
                try {

                    this.notifyGlobalRollback(branchLogs, globalLog.getTransId());
                    globalLog.setState(GlobalLogState.Rollbacked.getValue());
                    logStorage.deleteGlobalLog(globalLog.getTransId(), globalLog.getState());
                    logger.info("Update global transaction [TransId:{}] state to {}", tranId,
                        GlobalLogState.Rollbacked.name());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    globalLog.setState(GlobalLogState.RollbackFailed.getValue());
                    logStorage.updateGlobalLog(globalLog);
                    throw new DtsException("notify resourcemanager to commit failed");
                }

            }

            protected void notifyGlobalCommit(List<BranchLog> branchLogs, long transId) {
                if(!CollectionUtils.isEmpty(branchLogs)){
                    Collections.sort(branchLogs,(a,b)-> Long.valueOf(b.getBranchId()).compareTo(Long.valueOf(a.getBranchId())));
                }
                for (BranchLog branchLog : branchLogs) {
                    String clientAddress = branchLog.getResourceIp();
                    Long branchId = branchLog.getBranchId();
                    logger.info(
                        "Notify resource[clientAddress:{},branchId:{},resourceIp:{},resourceInfo:{}] to commit for transaction{}",
                        clientAddress, branchId, transId, branchLog.getResourceIp(), branchLog.getResourceInfo());
                    BranchCommitMessage branchCommitMessage = new BranchCommitMessage();
                    branchCommitMessage.setTranId(transId);
                    branchCommitMessage.setBranchId(branchId);
                    branchCommitMessage.setResourceInfo(branchLog.getResourceInfo());
                    branchCommitMessage.setResourceIp(branchLog.getResourceIp());
                    BranchCommitResultMessage branchCommitResult = null;
                    try {
                        branchCommitResult = messageSender.invokeSync(clientAddress, branchCommitMessage);
                    } catch (DtsException e) {
                        String message = "notify " + clientAddress + " commit occur system error,branchId:" + branchId;
                        logger.error(message, e);
                        throw new DtsException(e, message);
                    }
                    if (branchCommitResult != null) {
                        globalResultMessageHandler.processMessage(clientAddress, branchCommitResult);
                    } else {
                        throw new DtsException(
                            "notify " + clientAddress + " commit response null,branchId:" + branchId);
                    }
                }

            }

            protected void notifyGlobalRollback(List<BranchLog> branchLogs, long transId) {
                if(!CollectionUtils.isEmpty(branchLogs)){
                    Collections.sort(branchLogs,(a,b)-> Long.valueOf(b.getBranchId()).compareTo(Long.valueOf(a.getBranchId())));
                }
                for (int i = 0; i < branchLogs.size(); i++) {
                    BranchLog branchLog = branchLogs.get(i);
                    Long branchId = branchLog.getBranchId();
                    String clientAddress = branchLog.getResourceIp();
                    logger.info(
                        "Notify resource[clientAddress:{},branchId:{},resourceIp:{},resourceInfo:{}] to rollback for transaction{}",
                        clientAddress, branchId, transId, branchLog.getResourceIp(), branchLog.getResourceInfo());
                    BranchRollBackMessage branchRollbackMessage = new BranchRollBackMessage();
                    branchRollbackMessage.setTranId(transId);
                    branchRollbackMessage.setBranchId(branchId);
                    branchRollbackMessage.setResourceInfo(branchLog.getResourceInfo());
                    branchRollbackMessage.setResourceIp(branchLog.getResourceIp());
                    BranchRollbackResultMessage branchRollbackResult = null;
                    try {
                        branchRollbackResult = messageSender.invokeSync(clientAddress, branchRollbackMessage);
                    } catch (DtsException e) {
                        String message =
                            "notify " + clientAddress + " rollback occur system error,branchId:" + branchId;
                        logger.error(message, e);
                        throw new DtsException(e, message);
                    }
                    if (branchRollbackResult != null) {
                        globalResultMessageHandler.processMessage(clientAddress, branchRollbackResult);
                    } else {
                        throw new DtsException(
                            "notify " + clientAddress + " rollback response null,branchId:" + branchId);
                    }
                }
            }

            @Override
            public String processMessage(BeginMessage beginMessage, String clientIp) {
                GlobalLog globalLog = new GlobalLog();
                globalLog.setState(GlobalLogState.Begin.getValue());
                globalLog.setTimeout(beginMessage.getTimeout());
                globalLog.setClientIp(clientIp);
                globalLog.setClientInfo(beginMessage.getClientInfo());
                logStorage.insertGlobalLog(globalLog);
                long tranId = globalLog.getTransId();
                String xid = DtsXID.generateXID(tranId);
                logger.info("Created global log[TransId:{},xid:{}]", tranId, xid);
                return xid;
            }

            @Override
            public void processMessage(GlobalCommitMessage globalCommitMessage) {
                Long tranId = globalCommitMessage.getTranId();
                GlobalLog globalLog = logStorage.getGlobalLog(tranId);
                if (globalLog == null) {
                    throw new DtsException("transaction doesn't exist.");
                } else {
                    switch (GlobalLogState.parse(globalLog.getState())) {
                        case Begin:
                            logger.info("Trigger global transaction commit[TransId:{}]", tranId);
                            if (commitLogCache.get(tranId) == null) {
                                List<BranchLog> branchLogs = logStorage.getBranchLogs(tranId);
                                try {

                                    globalLog.setState(GlobalLogState.Commiting.getValue());
                                    logger.info("Update global transaction [TransId:{}] state to {}", tranId,
                                        GlobalLogState.Commiting.name());

                                    commitLogCache.set(tranId, globalLog, globalLog.getTimeout(),
                                        TimeUnit.MILLISECONDS);

                                    commitLogCache.addEntryListener(this, tranId, true);
                                    this.notifyGlobalCommit(branchLogs, globalLog.getTransId());
                                    globalLog.setState(GlobalLogState.Committed.getValue());
                                    logStorage.deleteGlobalLog(globalLog.getTransId(), globalLog.getState());
                                    logger.info("Update global transaction [TransId:{}] state to {}", tranId,
                                        GlobalLogState.Committed.name());

                                    commitLogCache.remove(tranId);
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                    globalLog.setState(GlobalLogState.CmmittedFailed.getValue());
                                    logStorage.updateGlobalLog(globalLog);
                                    throw new DtsException(e, "notify resourcemanager to commit failed");
                                }
                            }
                            return;
                        case Commiting:
                            throw new DtsException("Transaction is commiting!transactionId is:" + tranId);
                        default:
                            throw new DtsException("Unknown state " + globalLog.getState());
                    }

                }
            }

            @Override
            public void processMessage(GlobalRollbackMessage globalRollbackMessage) {
                long tranId = globalRollbackMessage.getTranId();
                GlobalLog globalLog = logStorage.getGlobalLog(tranId);
                if (globalLog == null) {
                    throw new DtsException("transaction doesn't exist.");
                } else {
                    switch (GlobalLogState.parse(globalLog.getState())) {
                        case Begin:
                            logger.info("Trigger global transaction rollback[TransId:{}]", tranId);
                            if (rollbackLogCache.get(tranId) == null) {
                                List<BranchLog> branchLogs = logStorage.getBranchLogs(tranId);
                                try {

                                    globalLog.setState(GlobalLogState.Rollbacking.getValue());
                                    logger.info("Update global transaction [TransId:{}] state to {}", tranId,
                                        GlobalLogState.Rollbacking.name());

                                    rollbackLogCache.set(tranId, globalLog, globalLog.getTimeout(),
                                        TimeUnit.MILLISECONDS);
                                    rollbackLogCache.addEntryListener(this, tranId, true);

                                    this.notifyGlobalRollback(branchLogs, globalLog.getTransId());
                                    globalLog.setState(GlobalLogState.Rollbacked.getValue());
                                    logStorage.deleteGlobalLog(globalLog.getTransId(), globalLog.getState());
                                    logger.info("Update global transaction [TransId:{}] state to {}", tranId,
                                        GlobalLogState.Rollbacked.name());
                                    rollbackLogCache.remove(tranId);
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                    globalLog.setState(GlobalLogState.RollbackFailed.getValue());
                                    logStorage.updateGlobalLog(globalLog);
                                    throw new DtsException("notify resourcemanager to commit failed");
                                }
                            }
                            return;
                        case Rollbacking:
                            throw new DtsException("Transaction is robacking!transactionId is:" + tranId);
                        default:
                            throw new DtsException("Unknown state " + globalLog.getState());
                    }
                }
            }

            @Override
            public void retryCommitFailed(GlobalLog globalLog) {
                logger.info("Retry CommitFailed global transaction[TransId:{}]", globalLog.getTransId());
                try {
                    if (!CollectionUtils.isEmpty(globalLog.getBranchLogs())) {
                        List<BranchLog> branch2Retry = globalLog.getBranchLogs().stream()
                            .filter((branchLog) -> BranchLogState.Success.getValue() != branchLog.getState())
                            .collect(Collectors.toList());
                        notifyGlobalCommit(branch2Retry, globalLog.getTransId());
                    }
                    globalLog.setState(GlobalLogState.Committed.getValue());
                    logStorage.updateGlobalLog(globalLog);
                    logger.info("Retry CommitFailed global transaction[TransId:{}] succeed", globalLog.getTransId());
                } catch (Throwable e) {
                    logger.error(String.format("Retry CommitFailed global transaction[TransId:%s] failed",
                        globalLog.getTransId()), e);
                }
            }

            @Override
            public void retryRollbackFailed(GlobalLog globalLog) {
                logger.info("Retry RollbackFailed global transaction[TransId:{}]", globalLog.getTransId());
                try {
                    if (!CollectionUtils.isEmpty(globalLog.getBranchLogs())) {
                        List<BranchLog> branch2Retry = globalLog.getBranchLogs().stream()
                            .filter((branchLog) -> BranchLogState.Success.getValue() != branchLog.getState())
                            .collect(Collectors.toList());
                        notifyGlobalRollback(branch2Retry, globalLog.getTransId());
                    }
                    globalLog.setState(GlobalLogState.Rollbacked.getValue());
                    logStorage.updateGlobalLog(globalLog);
                    logger.info("Retry RollbackFailed global transaction[TransId:{}] succeed", globalLog.getTransId());
                } catch (Throwable e) {
                    logger.error(String.format("Retry RollbackFailed global transaction[TransId:%s] failed",
                        globalLog.getTransId()), e);
                }
            }
        };
    }

    String processMessage(BeginMessage beginMessage, String clientIp);

    void processMessage(GlobalCommitMessage globalCommitMessage);

    void processMessage(GlobalRollbackMessage globalRollbackMessage);

    void retryCommitFailed(GlobalLog globalLog);

    void retryRollbackFailed(GlobalLog globalLog);

}
