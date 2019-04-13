package com.bkjk.platform.gray.resttemplate;

import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.http.HttpRequest;

import com.bkjk.platform.gray.AbstractGrayHeaderCustomizer;
import com.bkjk.platform.gray.GrayRulesStore;

public class RestTemplateGrayHeaderCustomizer extends AbstractGrayHeaderCustomizer<HttpRequest> {

    public RestTemplateGrayHeaderCustomizer(EurekaRegistration registration, GrayRulesStore grayRulesStore) {
        super(registration, grayRulesStore);
    }

    @Override
    protected void addHeaderToRequest(HttpRequest request, String key, String value) {
        request.getHeaders().set(key, value);
    }

    @Override
    protected boolean containsKey(HttpRequest request, String key) {
        return request.getHeaders().containsKey(key);
    }
}
