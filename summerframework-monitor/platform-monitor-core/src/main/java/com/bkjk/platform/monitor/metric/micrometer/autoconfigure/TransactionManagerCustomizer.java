package com.bkjk.platform.monitor.metric.micrometer.autoconfigure;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class TransactionManagerCustomizer {
    public static final Logger logger = LoggerFactory.getLogger(TransactionManagerCustomizer.class);
    private static final AtomicBoolean isDone = new AtomicBoolean(false);

    public void apply(Environment environment) {
        if (isDone.get()) {
            return;
        }
        isDone.set(true);
        if (!"false".equals(environment.getProperty("monitor.transaction", "true"))) {

            String[] dataSourceTransactionManagers =
                new String[] {"org.springframework.jdbc.datasource.DataSourceTransactionManager",
                    "org.springframework.orm.jpa.JpaTransactionManager",
                    "org.springframework.orm.hibernate5.HibernateTransactionManager"};
            ClassPool cp = ClassPool.getDefault();
            Arrays.asList(dataSourceTransactionManagers).forEach(m -> {
                CtClass cc = null;
                try {
                    logger.warn(
                        "Class {} will be modified for monitoring. You can disable it by set monitor.transaction = false ",
                        m);
                    cc = cp.get(m);
                    CtMethod doBeginMethod = cc.getDeclaredMethod("doBegin");
                    doBeginMethod.insertAfter(
                        "com.bkjk.platform.monitor.metric.micrometer.binder.db.TransactionUtil.recordBegin($1);");

                    CtMethod doRollbackMethod = cc.getDeclaredMethod("doRollback");
                    doRollbackMethod.insertAfter(
                        "com.bkjk.platform.monitor.metric.micrometer.binder.db.TransactionUtil.recordRollback($1);");

                    CtMethod doCommitMethod = cc.getDeclaredMethod("doCommit");
                    doCommitMethod.insertAfter(
                        "com.bkjk.platform.monitor.metric.micrometer.binder.db.TransactionUtil.recordCommit($1);");
                    Class c = cc.toClass();
                    logger.info("{} was modified", m);
                } catch (NotFoundException e) {
                    logger.warn("{} not found. Skip", m);
                } catch (CannotCompileException e) {
                    logger.error(e.getReason(), e);
                }
            });
        }
    }
}
