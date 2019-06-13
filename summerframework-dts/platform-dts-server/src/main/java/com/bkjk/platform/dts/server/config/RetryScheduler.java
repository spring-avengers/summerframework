package com.bkjk.platform.dts.server.config;

import com.bkjk.platform.dts.common.api.DtsServerMessageSender;
import com.bkjk.platform.dts.server.handler.ClientMessageHandler;
import com.bkjk.platform.dts.server.model.GlobalLog;
import com.bkjk.platform.dts.server.model.GlobalLogState;
import com.bkjk.platform.dts.server.storage.TransactionLogStorage;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "dts.retry.enabled", havingValue = "true")
public class RetryScheduler {

    @Autowired
    private HazelcastInstance hazelcatInstance;
    @Autowired
    private DtsServerMessageSender serverMessageSender;
    @Autowired
    private TransactionLogStorage dtsLogDao;
    private ILock committedFailedLock;
    private ILock rollbackFailedLock;
    private ClientMessageHandler clientHandler;
    Logger logger = LoggerFactory.getLogger(RetryScheduler.class);

    @PostConstruct
    public void init() {
        IMap<Long, GlobalLog> commitLogCache = hazelcatInstance.getMap(CacheConstant.COMMINTING_GLOBALLOG_CACHE);
        IMap<Long, GlobalLog> rollbackLogCache = hazelcatInstance.getMap(CacheConstant.ROLLBACKING_GLOBALLOG_CACHE);
        clientHandler = ClientMessageHandler.createClientMessageHandler(dtsLogDao, serverMessageSender, commitLogCache,
            rollbackLogCache);
        committedFailedLock = hazelcatInstance.getLock("CMMITTEDFAILEDLOCK");
        rollbackFailedLock = hazelcatInstance.getLock("ROLLBACKFAILEDLOCK");
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void retryCommitFailed() {
        boolean locked = false;
        try {
            if (locked = committedFailedLock.tryLock(0, TimeUnit.SECONDS, 30, TimeUnit.SECONDS)) {
                List<GlobalLog> globalLogs =
                    dtsLogDao.getGlobalTransactionToRetry(GlobalLogState.CmmittedFailed.getValue(), 10);
                if (!CollectionUtils.isEmpty(globalLogs)) {
                    globalLogs.forEach((globalLog) -> clientHandler.retryCommitFailed(globalLog));
                }
            }
        } catch (InterruptedException e) {
            logger.error("'CommittedFailed-Retry' try lock failed ", e);
        } finally {
            try {
                if (locked && committedFailedLock.isLocked()) {
                    committedFailedLock.unlock();
                }
            } catch (Throwable e) {
                logger.error("'CommittedFailed-Retry' unlock failed ", e);
            }
        }

    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void retryRollbackFailed() {
        boolean locked = false;
        try {
            if (locked = rollbackFailedLock.tryLock(1, TimeUnit.SECONDS, 30, TimeUnit.SECONDS)) {
                List<GlobalLog> globalLogs =
                    dtsLogDao.getGlobalTransactionToRetry(GlobalLogState.RollbackFailed.getValue(), 10);
                if (!CollectionUtils.isEmpty(globalLogs)) {
                    globalLogs.forEach((globalLog) -> clientHandler.retryRollbackFailed(globalLog));
                }
            }
        } catch (InterruptedException e) {
            logger.error("'RollbackFailed-Retry' try lock failed ", e);
        } finally {
            try {
                if (locked && rollbackFailedLock.isLocked()) {
                    rollbackFailedLock.unlock();
                }
            } catch (Throwable e) {
                logger.error("'RollbackFailed-Retry' unlock failed ", e);
            }
        }
    }

}
