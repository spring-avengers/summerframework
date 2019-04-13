package com.bkjk.platform.monitor.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bkjk.platform.monitor.logging.aop.RequestContext;

public class RequestUtil {

    private static final InheritableThreadLocal<HttpServletRequest> SERVERREQUEST_CACHE =
        new InheritableThreadLocal<HttpServletRequest>();

    public static void cleanCurrentHttpRequest() {
        SERVERREQUEST_CACHE.remove();
    }

    public static void setCurrentHttpRequest(HttpServletRequest request) {
        SERVERREQUEST_CACHE.set(request);
    }

    public HttpServletRequest getCurrentHttpRequest() {
        if (SERVERREQUEST_CACHE.get() != null) {
            HttpServletRequest request = SERVERREQUEST_CACHE.get();
            return request;
        } else {
            try {
                HttpServletRequest request = null;
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                if (requestAttributes != null && requestAttributes instanceof ServletRequestAttributes) {
                    request = ((ServletRequestAttributes)requestAttributes).getRequest();
                }
                return request;
            } catch (Throwable e) {
                return null;
            }
        }
    }

    public String getCurrentRequestUrl() {
        return this.getRequestUrl(getCurrentHttpRequest());
    }

    public String getCurrentRequestUrlOrDefault(String defaultValue) {
        try {
            String url = this.getRequestUrl(getCurrentHttpRequest());
            return StringUtils.isEmpty(url) ? defaultValue : url;
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public RequestContext getRequestContext() {
        HttpServletRequest request = getCurrentHttpRequest();
        return new RequestContext().add("url", getRequestUrl(request));
    }

    public String getRequestUrl(HttpServletRequest request) {
        return request == null ? null : request.getRequestURL().toString();
    }

}
