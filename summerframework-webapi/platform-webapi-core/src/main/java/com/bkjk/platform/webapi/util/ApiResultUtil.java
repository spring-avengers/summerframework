package com.bkjk.platform.webapi.util;

import com.bkjk.platform.webapi.result.ApiResultWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.server.ServerHttpResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/3/7 16:12
 **/
public class ApiResultUtil {
    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static final <T> T readValue(String string,Class<T> clazz){
        try {
            return mapper.readValue(string,clazz);
        } catch (IOException ignore) {
            throw new RuntimeException(ignore);
        }
    }

    public static final void addApiResultToResponseHeader(ServerHttpResponse response){

    }

    public static String encodeForHeader(String v){
        try {
            return  URLEncoder.encode(v,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 格式化 ApiResultWrapper 为JSON格式，不包括data。用于在header中传递异常信息
     * @param result
     * @return
     */
    public static final  String formatAsJSONWithoutData(ApiResultWrapper result){
        StringBuilder stringBuilder=new StringBuilder();
        stringBuilder.append("{");
        if(null!=result.getCode()){
            stringBuilder.append(String.format("\"code\":\"%s\",",result.getCode()));
        }
        if(null!=result.getError()){
            stringBuilder.append(String.format("\"error\":\"%s\",",result.getError()));
        }
        if(null!=result.getMessage()){
            stringBuilder.append(String.format("\"message\":\"%s\",",result.getMessage()));
        }
        if(null!=result.getPath()){
            stringBuilder.append(String.format("\"path\":\"%s\",",result.getPath()));
        }
//        if(null!=result.getData()){
//            stringBuilder.append(String.format("\"data\":\"%s\",",result.getData()));
//        }
        if(null!=result.getSchemaVersion()){
            stringBuilder.append(String.format("\"schemaVersion\":\"%s\",",result.getSchemaVersion()));
        }
        stringBuilder.append(String.format("\"time\":%s,",result.getTime()));
        stringBuilder.append(String.format("\"success\":%s",result.isSuccess()));
        stringBuilder.append("}");
        return encodeForHeader(stringBuilder.toString());
    }
}
