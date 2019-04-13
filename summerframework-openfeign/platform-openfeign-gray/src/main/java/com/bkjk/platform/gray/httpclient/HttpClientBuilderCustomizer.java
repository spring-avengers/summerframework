package com.bkjk.platform.gray.httpclient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpClientBuilderCustomizer {

    GrayHttpRequestInterceptor grayHttpRequestInterceptor;

    public HttpClientBuilderCustomizer(GrayHttpRequestInterceptor grayHttpRequestInterceptor) {
        this.grayHttpRequestInterceptor = grayHttpRequestInterceptor;
    }

    public CloseableHttpClient build(HttpClientBuilder builder) {
        builder.addInterceptorLast(grayHttpRequestInterceptor);
        return builder.build();
    }
}
