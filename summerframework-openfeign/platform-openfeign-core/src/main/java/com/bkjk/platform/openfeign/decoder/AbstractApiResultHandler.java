package com.bkjk.platform.openfeign.decoder;

import java.lang.reflect.Type;

import org.springframework.util.Assert;

import com.bkjk.platform.webapi.version.Constant;

import feign.Response;

public abstract class AbstractApiResultHandler implements ApiResultHandler {
    @Override
    public boolean support(Response response, Type type) {
        Assert.notNull(getSchemaVersion(), "ApiResultHandler.getSchemaVersion() CANNOT be null");
        return getSchemaVersion().equals(getFirstHeader(response, Constant.X_PLATFORM_SCHEMA_VERSION).orElse(""));
    }

    /**
     * 返回支持的schema的版本。框架根据版本确定应该使用此handler
     * 
     * @return
     */
    public abstract String getSchemaVersion();

}
