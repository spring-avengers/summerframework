package com.bkjk.platform.lock;

import com.bkjk.platform.lock.annotation.LockKey;
import com.bkjk.platform.lock.annotation.WithLock;
import com.bkjk.platform.lock.autoconfigure.LockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 13:14
 **/
@Aspect
@Order(0)
@Slf4j
public class LockAspect implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private LockConfiguration lockConfiguration;
    @Autowired
    private LockOperation lockOperation;

    private ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
    private ExpressionParser parser = new SpelExpressionParser();

    static final ThreadLocal<LockInstance> lockInfoThreadLocal = new ThreadLocal<>();

    private LockFactory getLockFactory(WithLock withLock) {
        LockFactory lockFactory = applicationContext.getBean(withLock.lockFactory(), LockFactory.class);
        Assert.notNull(lockFactory, "LockFactory not found. Want bean with name " + withLock.lockFactory());
        return lockFactory;
    }

    private LockInstance initLockInfo(ProceedingJoinPoint joinPoint, WithLock withLock) {
        // 获取key
        List<Object> keyList = new ArrayList<>();
        List<Object> originalKeys=new ArrayList<>();
        // 锁名
        if (StringUtils.isEmpty(withLock.name())) {
            keyList.add(String.format("%s.%s", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName()));
        } else {
            keyList.add(withLock.name());
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(),
                        method.getParameterTypes());
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
        // el表达式解析key
        for (String elKey : withLock.keys()) {
            if (elKey != null && !elKey.isEmpty()) {
                EvaluationContext context = new MethodBasedEvaluationContext(null, method, joinPoint.getArgs(), nameDiscoverer);
                Object key = parser.parseExpression(elKey).getValue(context);
                if (key != null) {
                    keyList.add(key);
                    originalKeys.add(key);
                }
            }
        }

        // 注解解析key
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            LockKey keyAnnotation = parameters[i].getAnnotation(LockKey.class);
            Object value = joinPoint.getArgs()[i];
            if (keyAnnotation != null && value != null) {
//                ClassUtils.isPrimitiveOrWrapper(value.getClass()); // 最好只支持基本类型和string
                keyList.add(value);
                originalKeys.add(value);
            }
        }

        String name=lockConfiguration.getPrefix() + keyList.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining("."));
        LockInstance lockInstance=lockOperation.requireLock(name,withLock.timeoutMillis(),withLock.expireTimeMillis(),withLock.lockType(),withLock.fair(),getLockFactory(withLock));
        if(!StringUtils.isEmpty(withLock.name())){
            lockInstance.setOriginalName(withLock.name());
        }
        if(originalKeys.size()>0){
            lockInstance.setOriginalKeys(originalKeys.stream().map(Object::toString).collect(Collectors.joining(".")));
        }
        return lockInstance;
    }

    @Around(value = "@annotation(withLock)")
    public Object around(ProceedingJoinPoint joinPoint, WithLock withLock) throws Throwable {
        WithLock synthesizedWithLock = AnnotationUtils.synthesizeAnnotation(withLock, null);
        LockInstance lockInstance=initLockInfo(joinPoint, synthesizedWithLock);
        lockInfoThreadLocal.set(lockInstance);
        AtomicReference<Object> returnValue = new AtomicReference<>();
        AtomicReference<Throwable> throwable = new AtomicReference<>();
        try {
            if (!StringUtils.isEmpty(synthesizedWithLock.lockFailedFallback())) {
                lockInstance.setLockFailedFallback((l) -> {
                    try {
                        returnValue.set(callFallback(synthesizedWithLock.lockFailedFallback(), joinPoint));
                    } catch (Throwable t) {
                        throwable.set(t);
                    }
                });
            }
            if (!StringUtils.isEmpty(synthesizedWithLock.unLockFailedFallback())) {
                lockInstance.setUnLockFailedFallback((l) -> {
                    try {
                        returnValue.set(callFallback(synthesizedWithLock.unLockFailedFallback(), joinPoint));
                    } catch (Throwable t) {
                        throwable.set(t);
                    }
                });
            }
            lockInstance.lockThen((l) -> {
                try {
                    returnValue.set(joinPoint.proceed());
                } catch (Throwable t) {
                    throwable.set(t);
                }
            });
        } finally {
            lockInfoThreadLocal.remove();
        }
        if (throwable.get() != null) {
            throw throwable.get();
        }
        return returnValue.get();
    }

    public Object callFallback(String methodName, ProceedingJoinPoint joinPoint) throws Throwable {
        Method fallbackMethod = ReflectionUtils.findMethod(joinPoint.getSignature().getDeclaringType(), methodName, ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterTypes());
        Assert.notNull(fallbackMethod, String.format("Method %s not found in %s", methodName, joinPoint.getSignature().getDeclaringType()));
        fallbackMethod.setAccessible(true);
        return ReflectionUtils.invokeMethod(fallbackMethod, joinPoint.getTarget(), joinPoint.getArgs());
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
