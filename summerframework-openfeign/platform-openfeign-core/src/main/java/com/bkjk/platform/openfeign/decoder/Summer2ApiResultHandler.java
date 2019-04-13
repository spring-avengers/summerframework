package com.bkjk.platform.openfeign.decoder;

import com.bkjk.platform.webapi.version.Constant;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Summer2ApiResultHandler extends AbstractApiResultHandler {
    final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getSchemaVersion() {
        return Constant.VERSION_SUMMER2;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
