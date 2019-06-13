package com.bkjk.platfrom.dts.core.interceptor;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.bkjk.platform.dts.common.DtsContext;
import com.bkjk.platfrom.dts.core.SpringCloudDtsContext;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class DtsRemoteInterceptor implements RequestInterceptor, HandlerInterceptor, ClientHttpRequestInterceptor {

    private static final String CONTEXT_HEADER_PARENT = "x-context-";

    @Override
    public void apply(RequestTemplate template) {
        Map<String, String> contexts = SpringCloudDtsContext.getContext().getAttachments();
        for (Map.Entry<String, String> entry : contexts.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            template.header(CONTEXT_HEADER_PARENT + key, value);
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {
        HttpRequestWrapper requestWrapper = new HttpRequestWrapper(request);
        Map<String, String> contexts = SpringCloudDtsContext.getContext().getAttachments();
        for (Map.Entry<String, String> entry : contexts.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            requestWrapper.getHeaders().set(CONTEXT_HEADER_PARENT + key, value);
        }
        return execution.execute(requestWrapper, body);
    }

    @Override
    public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
        throws Exception {
        DtsContext.getInstance().unbind();
    }

    @Override
    public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
        throws Exception {
        DtsContext.getInstance().unbind();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (StringUtils.startsWithIgnoreCase(headerName, CONTEXT_HEADER_PARENT)) {
                    String value = request.getHeader(headerName);
                    String key = StringUtils.replace(headerName, CONTEXT_HEADER_PARENT, "");
                    SpringCloudDtsContext.getContext().setAttachment(key.toUpperCase(), value);
                }
            }
        }
        return true;
    }

}
