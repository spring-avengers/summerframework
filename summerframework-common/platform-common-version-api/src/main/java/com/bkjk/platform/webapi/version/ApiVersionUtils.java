package com.bkjk.platform.webapi.version;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;

public class ApiVersionUtils {

    private final static String[] EMPTY = new String[0];

    public static String[] getApiVersionValues(ApiVersion classApiVersion, ApiVersion methodApiVersion) {
        ApiVersion version = methodApiVersion != null ? methodApiVersion : classApiVersion;
        if (version == null || version.value() == null || version.value().length == 0) {
            return EMPTY;
        }
        return version.value();
    }

    public static String[] getApiVersionValues(Class clazz, Method method) {
        return getApiVersionValues(AnnotationUtils.findAnnotation(clazz, ApiVersion.class),
            AnnotationUtils.findAnnotation(method, ApiVersion.class));
    }

    public static final String getFirstValue(Class clazz, Method method) {
        String[] values = getApiVersionValues(clazz, method);
        if (values == EMPTY) {
            return "";
        }
        return values[0];
    }
}
