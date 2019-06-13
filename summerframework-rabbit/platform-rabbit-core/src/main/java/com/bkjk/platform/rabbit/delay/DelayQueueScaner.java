
package com.bkjk.platform.rabbit.delay;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;

import com.google.common.collect.Lists;

public class DelayQueueScaner extends AbstractAutoProxyCreator {
    private static final long serialVersionUID = 1L;

    private final Set<String> proxyedSet = new HashSet<String>();

    private final RabbitTemplate rabbitTemplate;

    private DelayQueueInterceptor interceptor;

    public DelayQueueScaner(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    private Class<?> findTargetClass(Object proxy) throws Exception {
        if (AopUtils.isAopProxy(proxy)) {
            AdvisedSupport advised = this.getAdvisedSupport(proxy);
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

    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        try {
            Object beanCopy = bean;
            synchronized (proxyedSet) {
                if (proxyedSet.contains(beanName)) {
                    return beanCopy;
                }
                proxyedSet.add(beanName);
                Class<?> serviceInterface = this.findTargetClass(beanCopy);
                Method[] methods = serviceInterface.getMethods();
                List<Method> annotationMethods = Lists.newArrayList();
                for (Method method : methods) {
                    Delay anno = method.getAnnotation(Delay.class);
                    if (anno != null) {
                        annotationMethods.add(method);
                    }
                }
                if (!annotationMethods.isEmpty()) {
                    interceptor = new DelayQueueInterceptor(rabbitTemplate);
                } else {
                    return beanCopy;
                }
                if (!AopUtils.isAopProxy(beanCopy)) {
                    beanCopy = super.wrapIfNecessary(beanCopy, beanName, cacheKey);
                } else {
                    AdvisedSupport advised = this.getAdvisedSupport(beanCopy);
                    Advisor[] advisor = buildAdvisors(beanName, this.getAdvicesAndAdvisorsForBean(null, null, null));
                    for (Advisor avr : advisor) {
                        advised.addAdvisor(0, avr);
                    }
                }
                return beanCopy;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
