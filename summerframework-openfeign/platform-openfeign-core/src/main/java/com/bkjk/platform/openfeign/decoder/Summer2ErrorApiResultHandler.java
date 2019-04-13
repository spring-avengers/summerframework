package com.bkjk.platform.openfeign.decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.springframework.util.StringUtils;

import com.bkjk.platform.openfeign.exception.RemoteServiceException;
import com.bkjk.platform.webapi.version.Constant;

import feign.Response;
import feign.Util;

public class Summer2ErrorApiResultHandler extends Summer2ApiResultHandler {
    @Override
    public Object decode(Response response, Type type) throws IOException {
        // 将服务端的错误信息转化为异常
        String apiResultString = Util.toString(response.body().asReader());
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
        return super.support(response, type) && response.headers().containsKey(Constant.X_PLATFORM_ERROR);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
