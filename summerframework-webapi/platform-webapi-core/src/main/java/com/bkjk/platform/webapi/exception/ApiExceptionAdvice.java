package com.bkjk.platform.webapi.exception;

import com.bkjk.platform.webapi.ApiController;
import com.bkjk.platform.webapi.filter.ApiResponseFilter;
import com.bkjk.platform.webapi.result.ApiResultTransformer;
import com.bkjk.platform.webapi.result.ApiResultWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(annotations = ApiController.class)
public class ApiExceptionAdvice<T extends ApiResultWrapper> {

    @Value("${spring.application.name:}")
    private String name;

    @Autowired
    private ApiResultTransformer<T> apiResultTransformer;

    @Autowired
    private ApiResponseFilter<T> apiResponseFilter;

    @ExceptionHandler({Exception.class})
    public T handleException(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        if(StringUtils.isEmpty(ex.getMessage())){
            ex=new UnknownExceptionAndNoMessage(ex,"",String.format("%s from service [%s]",ex.getClass().getSimpleName(),name));
        }
        T result = apiResultTransformer.exceptionToResult(request, ex);
        result= apiResponseFilter.filter(result,(k,v)->response.setHeader(k,v));
        logOnError(request, ex,result);
        return result;
    }

    private void logOnError(HttpServletRequest request, Exception ex,T result) {
        if(!result.isSuccess()){
            log.error("Executing Api occurs error, path={}, error={}, message={},headers={} ", result.getPath(),result.getError(),result.getMessage(),Collections.list(request.getHeaderNames()).stream()
                    .collect(Collectors.toMap(h -> h, request::getHeader)), ex);
        }
    }

}
