package com.bkjk.platfrom.dts.core.client.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platfrom.dts.core.client.DtsTransaction;

public class DtsTransactionScaner extends AbstractAutoProxyCreator {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(DtsTransactionScaner.class);

    private final static Set<String> proxyedSet = new HashSet<String>();

    private TransactionDtsInterceptor interceptor;

    public DtsTransactionScaner() {
        logger.info("txc trasaction scaner initing....");
    }

    private Class<?> findTargetClass(Object proxy) throws Exception {
        if (AopUtils.isAopProxy(proxy)) {
            AdvisedSupport advised = getAdvisedSupport(proxy);
            Object target = advised.getTargetSource().getTarget();
            return findTargetClass(target);
        } else {
            return proxy.getClass();
        }
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
        TargetSource customTargetSource) throws BeansException {
        return new Object[] {interceptor};
    }

    private AdvisedSupport getAdvisedSupport(Object proxy) throws Exception {
        Field h;
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            h = proxy.getClass().getSuperclass().getDeclaredField("h");
        } else {
            h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        }
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return (AdvisedSupport)advised.get(dynamicAdvisedInterceptor);
    }

    private MethodDesc makeMethodDesc(Object bean, Method m) {
        DtsTransaction anno = m.getAnnotation(DtsTransaction.class);
        return new MethodDesc(anno, m);
    }

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        try {
            synchronized (proxyedSet) {
                if (proxyedSet.contains(beanName)) {
                    return bean;
                }
                proxyedSet.add(beanName);
                Class<?> serviceInterface = findTargetClass(bean);
                Method[] methods = serviceInterface.getMethods();
                LinkedList<MethodDesc> methodDescList = new LinkedList<MethodDesc>();
                for (Method method : methods) {
                    DtsTransaction anno = method.getAnnotation(DtsTransaction.class);
                    if (anno != null) {
                        methodDescList.add(makeMethodDesc(anno, method));
                    }
                }
                if (methodDescList.size() != 0) {
                    interceptor = new TransactionDtsInterceptor(methodDescList);
                } else {
                    return bean;
                }
                if (!AopUtils.isAopProxy(bean)) {
                    bean = super.wrapIfNecessary(bean, beanName, cacheKey);
                } else {
                    AdvisedSupport advised = getAdvisedSupport(bean);
                    Advisor[] advisor = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(null, null, null));
                    for (Advisor avr : advisor) {
                        advised.addAdvisor(0, avr);
                    }
                }
                return bean;
            }
        } catch (Exception e) {
            throw new DtsException(e);
        }
    }

}
