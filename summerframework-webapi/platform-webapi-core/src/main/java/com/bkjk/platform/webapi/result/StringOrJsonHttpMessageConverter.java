package com.bkjk.platform.webapi.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @Program: summerframework2
 * @Description: ApiResult改变了接口的返回值类型，需要对字符串类型单独做适配。否则字符串会被加上双引号
 * @Author: shaoze.wang
 * @Create: 2019/3/29 10:28
 **/
public class StringOrJsonHttpMessageConverter extends MappingJackson2HttpMessageConverter{
    public StringOrJsonHttpMessageConverter() {
    }

    public StringOrJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    private StringHttpMessageConverter stringHttpMessageConverter=new StringHttpMessageConverter();
    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        if(null!=object&& object instanceof String){
            outputMessage.getHeaders().setContentType(MediaType.TEXT_PLAIN);
            stringHttpMessageConverter.write(object==null?null:(String)object, MediaType.TEXT_PLAIN,outputMessage);
            return ;
        }
        super.writeInternal(object, type, outputMessage);
    }

}
