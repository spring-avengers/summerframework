package com.bkjk.platform.webapi.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiFiltersExclude.class)
public @interface ApiFilterExclude {
    Class<? extends AbstractApiFilter> value();
}
