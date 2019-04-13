package com.bkjk.platform.webapi.result;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

/**
 * @Program: summerframework2
 * @Description: 转换用户返回的body为指定的T类型。如果发生异常，必须转换异常为T类型的对象，保证接口返回值一致性
 * @Author: shaoze.wang
 * @Create: 2019/3/7 10:18
 **/
public interface ApiResultTransformer<T extends ApiResultWrapper> {

    /**
     * 修改成功请求的返回值
     * @param body
     * @param returnType
     * @param selectedContentType
     * @param selectedConverterType
     * @param request
     * @param response
     * @return
     */
    @Nullable
    T changeBody(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType,
                      Class<? extends HttpMessageConverter<?>> selectedConverterType,
                      ServerHttpRequest request, ServerHttpResponse response);

    /**
     * 修改失败请求的返回值
     * @param request
     * @param ex
     * @return
     */
    T exceptionToResult(HttpServletRequest request, Exception ex);

    /**
     * 返回值的类型。一定要返回实现类的类型，而***不是***ApiResultWrapper。因为FeignClient做转换时需要知道具体类型
     * @return
     */
    default Class<? extends ApiResultWrapper> getType(){
        return ApiResultWrapper.class;
    }
}
