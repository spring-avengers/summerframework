package com.bkjk.platfrom.dts.core.client.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platfrom.dts.core.client.DtsTransaction;

public class TransactionDtsInterceptor implements MethodInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionDtsInterceptor.class);
    private static final DtsTransactionTemplate template = new DtsTransactionTemplate();
    protected final static DtsTransaction NULL = new DtsTransaction() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }

        @Override
        public int timeout() {
            return 0;
        }
    };

    private static String formatMethod(Method method) {
        StringBuilder sb = new StringBuilder();

        String mehodName = method.getName();
        Class<?>[] params = method.getParameterTypes();
        sb.append(mehodName);
        sb.append("(");

        int paramPos = 0;
        for (Class<?> claz : params) {
            sb.append(claz.getName());
            if (++paramPos < params.length) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private volatile HashMap<Object, DtsTransaction> methodMap = new HashMap<Object, DtsTransaction>();

    public TransactionDtsInterceptor(List<MethodDesc> list) {
        for (MethodDesc md : list) {
            logger.info("add method " + md.getM().getName());
            methodMap.put(formatMethod(md.getM()), md.getTrasactionAnnotation());
        }
    }

    protected DtsTransaction getTxcTransaction(MethodInvocation arg) {
        DtsTransaction txc = methodMap.get(arg.getMethod());
        if (txc == null) {
            synchronized (this) {
                txc = methodMap.get(arg.getMethod());
                if (txc == null) {
                    String methodStringDesc = formatMethod(arg.getMethod());
                    txc = methodMap.get(methodStringDesc);
                    if (txc == null) {
                        txc = NULL;
                    }
                    HashMap<Object, DtsTransaction> newMap = new HashMap<Object, DtsTransaction>();
                    newMap.putAll(methodMap);
                    newMap.remove(methodStringDesc);
                    newMap.put(arg.getMethod(), txc);
                    methodMap = newMap;
                }
            }
        }
        return txc;
    }

    @Override
    public Object invoke(final MethodInvocation arg) throws Throwable {
        DtsTransaction annotaion = getTxcTransaction(arg);
        if (annotaion != NULL) {
            return template.run(new DtsCallback() {
                @Override
                public Object callback() throws Throwable {
                    return arg.proceed();
                }
            }, annotaion.timeout());
        }
        return arg.proceed();
    }
}
