package com.bkjk.platfrom.dts.core.client.aop;

import com.bkjk.platform.dts.common.DtsContext;
import com.bkjk.platfrom.dts.core.SpringContextHolder;
import com.bkjk.platfrom.dts.core.client.DtsClientTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class DtsTransactionTemplate {
    private DtsClientTransactionManager tm = new DtsClientTransactionManager();
    Logger logger = LoggerFactory.getLogger(DtsTransactionTemplate.class);

    private boolean isReady() {
        return SpringContextHolder.isEurekaLocalCacheRefreshed();
    }

    public Object run(DtsCallback callback, long timeout) throws Throwable {
        if (this.isReady()) {
            String xid = null;
            logger.info("Eureka localCache refreshed, start a global transaction ");
            try {
                logger.info("Begin a global transaction ");
                tm.begin(timeout);
                logger.info("Execute a global transaction ");
                xid = DtsContext.getInstance().getCurrentXid();
                Object obj = callback.callback();
                logger.info("Commit a global transaction ");
                tm.commit(xid);
                logger.info("A global transaction executed succeed");
                return obj;
            } catch (Throwable e) {
                if (!Objects.isNull(xid)) {
                    logger.error("Rollback a global transaction ", e);
                    try {
                        tm.rollback(xid);
                    } catch (Throwable ee) {
                        logger.error("Roll back a global transaction failed", ee);
                    }
                }
                throw e;
            } finally {
                DtsContext.getInstance().unbind();
            }
        } else {
            logger.info("Eureka localCache not refreshed, start a local transaction only");
            return callback.callback();
        }
    }
}
