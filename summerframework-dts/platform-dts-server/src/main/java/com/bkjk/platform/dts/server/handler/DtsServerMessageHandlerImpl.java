
package com.bkjk.platform.dts.server.handler;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.api.DtsServerMessageHandler;
import com.bkjk.platform.dts.common.api.DtsServerMessageSender;
import com.bkjk.platform.dts.common.protocol.header.BeginMessage;
import com.bkjk.platform.dts.common.protocol.header.BeginResultMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchRollBackMessage;
import com.bkjk.platform.dts.common.protocol.header.BranchRollbackResultMessage;
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
import com.bkjk.platform.dts.server.config.CacheConstant;
import com.bkjk.platform.dts.server.model.GlobalLog;
import com.bkjk.platform.dts.server.storage.TransactionLogStorage;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Component
@Scope("prototype")
public class DtsServerMessageHandlerImpl implements DtsServerMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(DtsServerMessageHandlerImpl.class);
    @Autowired
    private TransactionLogStorage dtsLogDao;

    @Autowired
    private DtsServerMessageSender serverMessageSender;

    @Autowired
    private HazelcastInstance hazelcatInstance;

    private IMap<Long, GlobalLog> commitLogCache;

    private IMap<Long, GlobalLog> rollbackLogCache;

    private ClientMessageHandler clientHandler;

    private ResouceMessageHandler resourceHandler;

    @Override
    public void handleMessage(String clientIp, BeginMessage message, BeginResultMessage resultMessage) {
        String xid = clientHandler.processMessage(message, clientIp);
        resultMessage.setXid(xid);
        logger.info("Started a global transaction Id：{}", xid);
    }

    @Override
    public void handleMessage(String clientIp, GlobalCommitMessage message, GlobalCommitResultMessage resultMessage) {
        resultMessage.setTranId(message.getTranId());
        clientHandler.processMessage(message);
        logger.info("Committed a global transaction Id：{}", message.getTranId());
    }

    @Override
    public void handleMessage(String clientIp, GlobalRollbackMessage message,
        GlobalRollbackResultMessage resultMessage) {
        resultMessage.setTranId(message.getTranId());
        clientHandler.processMessage(message);
        logger.info("Did rollback a global transaction Id：{}", message.getTranId());
    }

    @Override
    public void handleMessage(String clientIp, RegisterBranchMessage registerMessage,
        RegisterBranchResultMessage resultMessage) {
        long tranId = registerMessage.getTranId();
        Long branchId = resourceHandler.processMessage(registerMessage, clientIp);
        resultMessage.setBranchId(branchId);
        resultMessage.setTranId(tranId);
        logger.info("Register a branch[tranId:{},branch:{},resourceInfo:{}]", tranId, branchId,
            registerMessage.getResourceInfo());
    }

    @Override
    public void handleMessage(String clientIp, ClusterBranchCommitMessage message,
        ClusterBranchCommitResultMessage resultMessage) {
        BranchCommitMessage branchCommitMessage = message.getBranchCommitMessageObj();
        String resourceIp = branchCommitMessage.getResourceIp();
        BranchCommitResultMessage branchCommitResult = null;
        try {
            branchCommitResult = serverMessageSender.invokeSync(resourceIp, branchCommitMessage);
        } catch (DtsException e) {
            String errorMessage =
                "notify " + resourceIp + " commit occur system error,branchId:" + branchCommitMessage.getBranchId();
            logger.error(errorMessage, e);
            throw new DtsException(e, errorMessage);
        }
        if (branchCommitResult != null) {
            resultMessage.setBranchCommitResultMessage(branchCommitResult);
        } else {
            throw new DtsException(
                "notify " + resourceIp + " commit response null,branchId:" + branchCommitMessage.getBranchId());
        }
    }

    @Override
    public void handleMessage(String clientIp, ClusterBranchRollBackMessage message,
        ClusterBranchRollbackResultMessage resultMessage) {
        BranchRollBackMessage branchRollbackMessage = message.getBranchRollBackMessageObj();
        String resourceIp = branchRollbackMessage.getResourceIp();
        BranchRollbackResultMessage branchRollbackResult = null;
        try {
            branchRollbackResult = serverMessageSender.invokeSync(resourceIp, branchRollbackMessage);
        } catch (DtsException e) {
            String errorMessage =
                "notify " + resourceIp + " commit occur system error,branchId:" + branchRollbackMessage.getBranchId();
            logger.error(errorMessage, e);
            throw new DtsException(e, errorMessage);
        }
        if (branchRollbackResult != null) {
            resultMessage.setBranchRollbackResultMessage(branchRollbackResult);
        } else {
            throw new DtsException(
                "notify " + resourceIp + " commit response null,branchId:" + branchRollbackMessage.getBranchId());
        }
    }

    @PostConstruct
    public void init() {
        commitLogCache = hazelcatInstance.getMap(CacheConstant.COMMINTING_GLOBALLOG_CACHE);
        rollbackLogCache = hazelcatInstance.getMap(CacheConstant.ROLLBACKING_GLOBALLOG_CACHE);
        clientHandler = ClientMessageHandler.createClientMessageHandler(dtsLogDao, serverMessageSender, commitLogCache,
            rollbackLogCache);
        resourceHandler = ResouceMessageHandler.createResouceMessageHandler(dtsLogDao, serverMessageSender);

    }

}
