package com.bkjk.platform.webapi.filter;

import com.bkjk.platform.webapi.result.ApiResultWrapper;
import com.bkjk.platform.webapi.util.ApiResultUtil;
import com.bkjk.platform.webapi.version.Constant;

import java.util.Optional;
import java.util.function.BiConsumer;

import static com.bkjk.platform.webapi.version.Constant.*;

/**
 * @Program: summerframework2
 * @Description: 无论返回的结果是否成功，都会走这个filter
 * @Author: shaoze.wang
 * @Create: 2019/3/8 10:06
 **/
public interface ApiResponseFilter<T extends ApiResultWrapper> {

    T filter(T apiResult, BiConsumer<String,String> responseHeaderSetter);

    class Default<T extends ApiResultWrapper> implements ApiResponseFilter<T>{

        @Override
        public T filter(T apiResult, BiConsumer<String, String> responseHeaderSetter) {
            if(apiResult==null){
                return apiResult;
            }
            // 在header里放上
            responseHeaderSetter.accept(X_PLATFORM_SCHEMA,TRUE_STRING);
            responseHeaderSetter.accept(X_PLATFORM_SCHEMA_VERSION, Optional.ofNullable(apiResult.getSchemaVersion()).orElse(""));
            if(!apiResult.isSuccess()){
                responseHeaderSetter.accept(X_PLATFORM_ERROR,TRUE_STRING);
                // 如果请求有异常，且用户自定义了返回格式，那么框架将无法从body里解析异常信息，所以
                // 把返回值（不包括data）按summer2的格式放到header里。这样框架才能在全链路上正确处理异常
                if(!Constant.VERSION_SUMMER2.equals(apiResult.getSchemaVersion())){
                    responseHeaderSetter.accept(X_PLATFORM_SCHEMA_BODY, ApiResultUtil.formatAsJSONWithoutData(apiResult));
                }
            }
            return apiResult;
        }
    }
}
