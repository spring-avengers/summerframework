package com.bkjk.platform.gray.feign;

import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import com.bkjk.platform.gray.AbstractGrayHeaderCustomizer;
import com.bkjk.platform.gray.GrayRulesStore;

import feign.RequestTemplate;

public class OpenFeignGrayHeaderCustomizer extends AbstractGrayHeaderCustomizer<RequestTemplate> {

    public OpenFeignGrayHeaderCustomizer(EurekaRegistration registration, GrayRulesStore grayRulesStore) {
        super(registration, grayRulesStore);
    }

    @Override
    public void addHeaderToRequest(RequestTemplate request, String key, String value) {
        request.header(key, value);
    }

    @Override
    public boolean containsKey(RequestTemplate request, String key) {
        return request.headers().containsKey(key);
    }
}
