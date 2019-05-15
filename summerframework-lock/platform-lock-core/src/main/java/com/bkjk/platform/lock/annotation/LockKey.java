package com.bkjk.platform.lock.annotation;

import java.lang.annotation.*;

/**
 * @Program: summerframework2
 * @Description: 指定参数作为缓存key
 * @Author: shaoze.wang
 * @Create: 2019/5/6 9:55
 **/
@Target(value = {ElementType.PARAMETER})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface LockKey {
}
