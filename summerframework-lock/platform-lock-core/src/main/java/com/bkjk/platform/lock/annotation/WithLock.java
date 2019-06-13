package com.bkjk.platform.lock.annotation;

import com.bkjk.platform.lock.LockType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

import static com.bkjk.platform.lock.autoconfigure.LockAutoConfiguration.DEFAULT_LOCK_FACTORY_BEAN;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/5/6 9:34
 **/
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface WithLock {
    /**
     * 锁的名称，支持spring el表达式
     * @return
     */
    @AliasFor("value")
    String name() default "";

    /**
     * 锁的名称，支持spring el表达式
     * @return
     */
    @AliasFor("name")
    String value() default "";

    /**
     * 是否公平锁
     * @return
     */
    boolean fair() default false;

    /**
     * 获取锁的超时时间，单位毫秒。默认不超时无限等待
     * @return
     */
    long timeoutMillis() default Long.MAX_VALUE;

    /**
     * 锁续期的时间，单位毫秒。定时续期锁时会用到它，有些锁的实现上是没有失效时间的，比如Java自带的重入锁，只要获得锁就一直持有，不存在失效时间。
     * @return
     */
    long expireTimeMillis() default 30_000;

    String [] keys() default {};

    /**
     * 创建锁的bean的名称
     * @return
     */
    String lockFactory() default DEFAULT_LOCK_FACTORY_BEAN;

    /**
     * 锁的类型
     * @return
     */
    LockType lockType() default LockType.DEFAULT;

    /**
     * 加锁失败后执行指定名称的参数完全一致的方法，方法不存在会抛出异常
     * @return
     */
    String lockFailedFallback() default "";

    /**
     * 解锁失败后执行指定名称的参数完全一致的方法，方法不存在会抛出异常
     * @return
     */
    String unLockFailedFallback() default "";

}
