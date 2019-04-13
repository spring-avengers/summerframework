package com.bkjk.platform.webapi;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @Program: summerframework2
 * @Description: 用法同 ApiController
 * @Author: shaoze.wang
 * @Create: 2019/3/7 13:53
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiController
public @interface RemoteService {
    @AliasFor(annotation = ApiController.class)
    String value() default "";
}
