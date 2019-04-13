package com.bkjk.platform.gray.httpclient;

import org.apache.http.HttpRequest;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;

import com.bkjk.platform.gray.AbstractGrayHeaderCustomizer;
import com.bkjk.platform.gray.GrayRulesStore;

public class HttpClientGrayHeaderCustomizer extends AbstractGrayHeaderCustomizer<HttpRequest> {

    public HttpClientGrayHeaderCustomizer(EurekaRegistration registration, GrayRulesStore grayRulesStore) {
        super(registration, grayRulesStore);
    }

    @Override
    protected void addHeaderToRequest(HttpRequest request, String key, String value) {
        request.setHeader(key, value);
    }

    @Override
    protected boolean containsKey(HttpRequest request, String key) {
        return request.containsHeader(key);
    }
}
