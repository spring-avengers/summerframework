package com.bkjk.platform.openfeign.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;

public class ApiErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        return super.decode(methodKey, response);
    }
}
