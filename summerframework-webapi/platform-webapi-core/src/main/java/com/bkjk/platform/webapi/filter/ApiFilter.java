package com.bkjk.platform.webapi.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiFilters.class)
public @interface ApiFilter {
    int order() default 0;

    Class<? extends AbstractApiFilter> value();
}
