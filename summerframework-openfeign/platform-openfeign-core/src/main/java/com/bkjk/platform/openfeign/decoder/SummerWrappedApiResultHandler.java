package com.bkjk.platform.openfeign.decoder;

import static com.bkjk.platform.webapi.version.Constant.X_PLATFORM_SCHEMA;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.springframework.cloud.openfeign.support.SpringDecoder;

import com.bkjk.platform.webapi.result.ApiResultWrapper;

import feign.Response;

public class SummerWrappedApiResultHandler implements ApiResultHandler {

    Class<? extends ApiResultWrapper> apiResultType;
    final SpringDecoder springDecoder;

    public SummerWrappedApiResultHandler(Class<? extends ApiResultWrapper> apiResultType, SpringDecoder springDecoder) {
        this.apiResultType = apiResultType;
        this.springDecoder = springDecoder;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        ApiResultWrapper result = (ApiResultWrapper)springDecoder.decode(response, new ParameterizedType() {

            @Override
            public String getTypeName() {
                return apiResultType.getTypeName();
            }

            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {type};
            }

            @Override
            public Type getRawType() {
                return apiResultType;
            }

            @Override
            public Type getOwnerType() {
                return apiResultType;
            }
        });
        return result.getData();
    }

    @Override
    public boolean support(Response response, Type type) {
        return response.headers().containsKey(X_PLATFORM_SCHEMA) && !apiResultType.equals(type);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
