package com.bkjk.platform.openfeign.decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.springframework.util.StringUtils;

import com.bkjk.platform.openfeign.exception.RemoteServiceException;
import com.bkjk.platform.webapi.version.Constant;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;

public class SummerErrorAdapterApiResultHandler implements ApiResultHandler {
    final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object decode(Response response, Type type) throws IOException {
        String apiResultString = getFirstHeader(response, Constant.X_PLATFORM_SCHEMA_BODY).orElse("");
        if (!StringUtils.isEmpty(apiResultString)) {
            HashMap apiResult = mapper.readValue(apiResultString, HashMap.class);
            String errorMessage = getStringFromMap(apiResult, "message");
            throw new RemoteServiceException(getStringFromMap(apiResult, "code"),
                StringUtils.isEmpty(errorMessage) ? getStringFromMap(apiResult, "error") : errorMessage);
        }
        return null;
    }

    @Override
    public boolean support(Response response, Type type) {
        return response.headers().containsKey(Constant.X_PLATFORM_ERROR) && !Constant.VERSION_SUMMER2
            .equals(getFirstHeader(response, Constant.X_PLATFORM_SCHEMA_VERSION).orElse(""));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
