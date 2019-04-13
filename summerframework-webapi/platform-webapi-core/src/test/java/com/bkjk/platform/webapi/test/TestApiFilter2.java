package com.bkjk.platform.webapi.test;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.bkjk.platform.webapi.filter.AbstractApiFilter;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TestApiFilter2 extends AbstractApiFilter {
    @Override
    public Object beforeReturn(HttpServletRequest request, HttpServletResponse response, Method method, Object object) {
        if (object instanceof MyItem) {
            MyItem myItem = (MyItem)object;
            myItem.setName(myItem.getName() + getDescription());
            return myItem;
        }
        return object;
    }

    @Override
    protected String getDescription() {
        return "testfilter2";
    }
}
