package com.bkjk.platform.webapi.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.bkjk.platform.webapi.ApiController;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ApiFilterInterceptor implements HandlerInterceptor {
    @Autowired
    ApplicationContext applicationContext;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        ModelAndView modelAndView) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            if (handlerMethod.getMethod().getDeclaringClass().getAnnotationsByType(ApiController.class).length > 0) {
                for (AbstractApiFilter filterInstance : ApiFilterUtil.getFilters(applicationContext,
                    handlerMethod.getMethod(), true)) {
                    filterInstance.postActionHandler(request, response, handlerMethod.getMethod());
                }
            }
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod)handler;
            if (handlerMethod.getMethod().getDeclaringClass().getAnnotationsByType(ApiController.class).length > 0) {
                for (AbstractApiFilter filterInstance : ApiFilterUtil.getFilters(applicationContext,
                    handlerMethod.getMethod(), false)) {
                    if (!filterInstance.preActionHandler(request, response, handlerMethod.getMethod()))
                        return false;
                }
            }
        }

        return true;
    }
}
