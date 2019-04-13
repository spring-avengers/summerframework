package com.bkjk.platform.gray.resttemplate;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class GrayClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    RestTemplateGrayHeaderCustomizer restTemplateGrayHeaderCustomizer;

    public GrayClientHttpRequestInterceptor(RestTemplateGrayHeaderCustomizer restTemplateGrayHeaderCustomizer) {
        this.restTemplateGrayHeaderCustomizer = restTemplateGrayHeaderCustomizer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {
        restTemplateGrayHeaderCustomizer.apply(request);
        return execution.execute(request, body);
    }
}
