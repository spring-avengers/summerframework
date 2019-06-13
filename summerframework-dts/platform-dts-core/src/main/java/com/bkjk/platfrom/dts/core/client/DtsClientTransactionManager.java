package com.bkjk.platfrom.dts.core.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.bkjk.platform.dts.common.DtsContext;
import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.DtsXID;
import com.bkjk.platform.dts.common.model.ClientInfo;
import com.bkjk.platform.dts.common.protocol.header.BeginMessage;
import com.bkjk.platform.dts.common.protocol.header.BeginResultMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalCommitMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalCommitResultMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalRollbackMessage;
import com.bkjk.platform.dts.common.protocol.header.GlobalRollbackResultMessage;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.bkjk.platfrom.dts.core.SpringContextHolder;

public class DtsClientTransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(DtsClientTransactionManager.class);

    private final DefaultDtsClientMessageSender dtsClientMessageSender = new DefaultDtsClientMessageSender();

    private final Environment env;

    public DtsClientTransactionManager() {
        dtsClientMessageSender.start();
        this.env = SpringContextHolder.getBean(Environment.class);
    }

    public void begin(final long timeout) throws DtsException {
        BeginMessage beginMessage = new BeginMessage();
        beginMessage.setTimeout(timeout);
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setAppName(env.getProperty("spring.application.name"));
        clientInfo.setIp(NetWorkUtil.getLocalIp());
        beginMessage.setClientInfo(JsonUtil.toJson(clientInfo));
        try {
            BeginResultMessage beginResultMessage = dtsClientMessageSender.invoke(beginMessage);
            String transId = beginResultMessage.getXid();
            logger.info("Request a global transaction[transId:{}]", transId);
            DtsContext.getInstance().bind(transId);
        } catch (Throwable th) {
            logger.error("Request a global transaction failed ", th);
            throw new DtsException(th);
        }
    }

    public void commit(String xid) throws DtsException {
        GlobalCommitMessage commitMessage = new GlobalCommitMessage();
        if (xid == null) {
            throw new DtsException("the thread is not in transaction when invoke commit.");
        }
        long transId = DtsXID.getTransactionId(xid);
        commitMessage.setTranId(transId);
        long start = 0;
        if (logger.isDebugEnabled())
            start = System.currentTimeMillis();
        try {
            GlobalCommitResultMessage resultMessage = null;
            Exception ex = null;
            try {
                logger.info("Request a global transaction[transId:{},xid:{}] commit ", transId, xid);
                resultMessage = (GlobalCommitResultMessage)dtsClientMessageSender.invoke(commitMessage);
            } catch (Exception e) {
                logger.error(
                    String.format("Request a global transaction[transId:%s,xid:%s] commit failed", transId, xid), e);
                ex = e;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                }
            }
            if (resultMessage == null) {
                throw new DtsException("transaction " + xid + " Global commit failed.server response is null");
            }
            if (ex != null) {
                throw new DtsException(ex, "transaction " + xid + " Global commit failed.");
            }
        } finally {
            if (logger.isDebugEnabled()) {
                long end = System.currentTimeMillis();
                logger.debug("send global commit message:" + commitMessage + " cost " + (end - start) + " ms.");
            } else
                logger.info("send global commit message to dts server, xid:" + xid);
        }

    }

    public void rollback(String xid) throws DtsException {
        GlobalRollbackMessage rollbackMessage = new GlobalRollbackMessage();
        long transId = DtsXID.getTransactionId(xid);
        rollbackMessage.setTranId(transId);
        long start = 0;
        if (logger.isDebugEnabled())
            start = System.currentTimeMillis();
        GlobalRollbackResultMessage resultMessage = null;
        try {
            Exception ex = null;
            try {
                logger.info("Request a global transaction[transId:{},xid:{}] rollback ", transId, xid);
                resultMessage = (GlobalRollbackResultMessage)dtsClientMessageSender.invoke(rollbackMessage);
            } catch (Exception e) {
                logger.error(
                    String.format("Request a global transaction[transId:%s,xid:%s] rollback failed", transId, xid), e);
                ex = e;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                }
            }
            if (resultMessage == null) {
                throw new DtsException("transaction " + xid + " Global rollback failed.server response is null");
            }
            if (ex != null) {
                throw new DtsException(ex, "transaction " + xid + " Global rollback failed.");
            }
        } finally {
            if (logger.isDebugEnabled()) {
                long end = System.currentTimeMillis();
                logger.debug("invoke global rollback message:" + rollbackMessage + " cost " + (end - start) + " ms.");
            } else
                logger.info("send global rollback message to dts server, xid:" + xid);
        }

    }

}
