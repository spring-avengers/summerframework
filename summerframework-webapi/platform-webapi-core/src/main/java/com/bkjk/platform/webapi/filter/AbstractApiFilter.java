package com.bkjk.platform.webapi.filter;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractApiFilter {

    protected Object beforeReturn(HttpServletRequest request, HttpServletResponse response, Method method,
        Object object) {
        return object;
    }

    public final Object beforeReturnHandler(HttpServletRequest request, HttpServletResponse response, Method method,
        Object object) {
        long begin = System.currentTimeMillis();
        Object o = beforeReturn(request, response, method, object);
        try {
            if (Arrays.asList(getClass().getDeclaredMethods()).stream()
                .anyMatch(m -> m.getName().equals("beforeReturn")))
                log.info(String.format("Took [%s] to execute filter [%s:%s] phase [%s] on %s (method:[%s])",
                    (System.currentTimeMillis() - begin) + "ms", this.getClass().toString(), getDescription(),
                    "beforeReturn", request.getRequestURI(), method.toGenericString()));
        } catch (Exception ex) {
        }
        return o;
    }

    protected abstract String getDescription();

    protected void postAction(HttpServletRequest request, HttpServletResponse response, Method method) {
    }

    public final void postActionHandler(HttpServletRequest request, HttpServletResponse response, Method method) {
        long begin = System.currentTimeMillis();
        postAction(request, response, method);
        try {
            if (Arrays.asList(getClass().getDeclaredMethods()).stream().anyMatch(m -> m.getName().equals("postAction")))
                log.info(String.format("Took [%s] to execute filter [%s:%s] phase [%s] on %s (method:[%s])",
                    (System.currentTimeMillis() - begin) + "ms", this.getClass().toString(), getDescription(),
                    "postAction", request.getRequestURI(), method.toGenericString()));
        } catch (Exception ex) {
        }
    }

    protected boolean preAction(HttpServletRequest request, HttpServletResponse response, Method method) {
        return true;
    }

    public final boolean preActionHandler(HttpServletRequest request, HttpServletResponse response, Method method) {
        long begin = System.currentTimeMillis();
        boolean c = false;
        try {
            c = preAction(request, response, method);
        } finally {
            try {
                if (Arrays.asList(getClass().getDeclaredMethods()).stream()
                    .anyMatch(m -> m.getName().equals("preAction")))
                    log.info(String.format("Took [%s] to execute filter [%s:%s] phase [%s] on %s (method:[%s]) => %s",
                        (System.currentTimeMillis() - begin) + "ms", this.getClass().toString(), getDescription(),
                        "preAction", request.getRequestURI(), method.toGenericString(), c ? "Continue" : "Break"));
            } catch (Exception ex) {
            }
        }
        return c;

    }
}
