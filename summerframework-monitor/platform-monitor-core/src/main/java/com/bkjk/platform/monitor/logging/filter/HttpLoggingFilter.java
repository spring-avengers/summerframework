package com.bkjk.platform.monitor.logging.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpLoggingFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        long start = System.currentTimeMillis();
        HttpServletRequest originalRequest = null;
        RequestWrapper requestWrapper = null;
        try {
            originalRequest = (HttpServletRequest)request;
            requestWrapper = new RequestWrapper(originalRequest);
            logRequest(requestWrapper, originalRequest);
            chain.doFilter(requestWrapper, response);
        } finally {
            long elapsedTime = System.currentTimeMillis() - start;
            if (null != originalRequest && null != requestWrapper
                && !originalRequest.getRequestURI().contains("health")) {
                logger.info("remoteAddress={}|requestURL={}|method={}|elapsedTime={}", requestWrapper.getRemoteAddr(),
                    requestWrapper.getRequestURL(), requestWrapper.getMethod(), elapsedTime);
            }
        }

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    private boolean logBody(RequestWrapper request) {
        String method = request.getMethod().toUpperCase();
        return (HTTPConstants.METHOD_POST.equals(method) || HTTPConstants.METHOD_PUT.equals(method))
            && !request.isMultipart();
    }

    private void logHeaders(HttpServletRequest request, String url, String method) {
        Enumeration<?> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headerName = (String)headers.nextElement();
            logger.info("url = {}, method= {}, [header] {}={}", url, method, headerName, request.getHeader(headerName));
        }
    }

    private void logParameters(HttpServletRequest request, String url, String method) {
        Enumeration<?> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String)paramNames.nextElement();
            logger.info("url = {}, method= {}, [param] {}={}", url, method, paramName, request.getParameter(paramName));
        }
    }

    private void logRequest(RequestWrapper requestWrapper, HttpServletRequest originalRequest) throws IOException {
        final String url = requestWrapper.getRequestURL().toString();
        final String method = requestWrapper.getMethod();
        logger.info("requestURL={}", url);
        logger.info("method={}", method);
        logHeaders(originalRequest, url, method);
        logParameters(originalRequest, url, method);
        logger.info("remoteAddress={}", requestWrapper.getRemoteAddr());
        if (logBody(requestWrapper)) {
            logger.info("url = {}, method= {}, body={}", url, method, requestWrapper.getOriginalBody());
        }
    }

}
