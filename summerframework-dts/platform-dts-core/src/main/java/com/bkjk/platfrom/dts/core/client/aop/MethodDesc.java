package com.bkjk.platfrom.dts.core.client.aop;

import java.lang.reflect.Method;

import com.bkjk.platfrom.dts.core.client.DtsTransaction;

public class MethodDesc {
    private DtsTransaction trasactionAnnotation;
    private Method m;

    public MethodDesc(DtsTransaction trasactionAnnotation, Method m) {
        super();
        this.trasactionAnnotation = trasactionAnnotation;
        this.m = m;
    }

    public Method getM() {
        return m;
    }

    public DtsTransaction getTrasactionAnnotation() {
        return trasactionAnnotation;
    }

    public void setM(Method m) {
        this.m = m;
    }

    public void setTrasactionAnnotation(DtsTransaction trasactionAnnotation) {
        this.trasactionAnnotation = trasactionAnnotation;
    }

}
