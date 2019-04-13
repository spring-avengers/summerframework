package com.bkjk.platform.webapi.test;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.bkjk.platform.webapi.filter.AbstractApiFilter;

@Component
public class LoginCheck extends AbstractApiFilter {

    @Override
    protected String getDescription() {
        return "校验Token";
    }

    @Override
    public boolean preAction(HttpServletRequest request, HttpServletResponse response, Method method) {
        if (request.getHeader("token") == null || !request.getHeader("token").equals("1"))
            throw new RuntimeException("请登录！");
        return true;
    }
}
