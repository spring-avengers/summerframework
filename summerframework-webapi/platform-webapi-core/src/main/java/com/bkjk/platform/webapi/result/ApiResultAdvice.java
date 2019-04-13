package com.bkjk.platform.webapi.result;

import com.bkjk.platform.webapi.ApiController;
import com.bkjk.platform.webapi.filter.ApiResponseFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = ApiController.class)
public class ApiResultAdvice implements ResponseBodyAdvice {


    @Autowired
    private ApiResultTransformer apiResultTransformer;

    @Autowired
    private ApiResponseFilter apiResponseFilter;

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        Object result = apiResultTransformer.changeBody(body, returnType, selectedContentType, selectedConverterType, request, response);
        if (result != null && result instanceof ApiResultWrapper) {
            ApiResultWrapper apiResultWrapper = (ApiResultWrapper) result;
            result= apiResponseFilter.filter(apiResultWrapper,(k,v)->response.getHeaders().set(k.toString(),v.toString()));
        }
        return result;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return AnnotationUtils.findAnnotation(returnType.getMethod(), NoApiResult.class) == null
                && AnnotationUtils.findAnnotation(returnType.getDeclaringClass(), NoApiResult.class) == null;
    }
}
