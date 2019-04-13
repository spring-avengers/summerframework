package com.bkjk.platform.openfeign.decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.Ordered;

import feign.Response;

public interface ApiResultHandler extends Ordered {

    /**
     * 返回null表示继续交友下一个handler处理。该方法返回任意值，则以它作为最终解码出的对象。
     * 
     * @param response
     * @param type
     * @return
     * @throws IOException
     */
    Object decode(Response response, Type type) throws IOException;

    /**
     * 判断是否支持处理当前response
     * 
     * @param response
     * @param type
     * @return
     */
    boolean support(Response response, Type type);

    default Optional<String> getFirstHeader(Response response, String key) {
        return response.headers().getOrDefault(key, new ArrayList<>()).stream().findFirst();
    }

    default String getStringFromMap(Map map, String key) {
        Object value = map.get(key);
        return null == value ? "" : value.toString();
    }

}
