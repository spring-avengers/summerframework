package com.bkjk.platform.openfeign.decoder;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.Response;
import feign.Util;
import feign.codec.Decoder;

public class ApiResultDecoder implements Decoder {
    final ObjectMapper mapper;

    final List<ApiResultHandler> apiResultHandlers;

    final Decoder delegate;

    public ApiResultDecoder(Decoder delegate, List<ApiResultHandler> apiResultHandlers) {
        Assert.notEmpty(apiResultHandlers, "List<ApiResultHandler> must not be empty");
        Objects.requireNonNull(delegate, "Decoder must not be null. ");
        this.delegate = delegate;
        mapper = new ObjectMapper();
        this.apiResultHandlers = apiResultHandlers;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        for (ApiResultHandler apiResultHandler : apiResultHandlers) {
            if (apiResultHandler.support(response, type)) {
                Object obj = apiResultHandler.decode(response, type);
                if (null != obj) {
                    return obj;
                }
            }
        }
        if (!isOptional(type)) {
            return delegate.decode(response, type);
        }
        if (response.status() == 404 || response.status() == 204) {
            return Optional.empty();
        }
        Type enclosedType = Util.resolveLastTypeParameter(type, Optional.class);
        return Optional.of(delegate.decode(response, enclosedType));
    }

    static boolean isOptional(Type type) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType)type;
        return parameterizedType.getRawType().equals(Optional.class);
    }

    private Optional<String> getFirstHeader(Response response, String key) {
        return response.headers().getOrDefault(key, new ArrayList<>()).stream().findFirst();
    }

    private String getStringFromMap(Map map, String key) {
        Object value = map.get(key);
        return null == value ? "" : value.toString();
    }
}
