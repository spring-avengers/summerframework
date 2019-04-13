package com.bkjk.platform.monitor.metric.micrometer.binder.resttemplate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

public class RestTemplatePostProcessor implements RestTemplateCustomizer {
    final MetricsClientHttpRequestInterceptor metricsClientHttpRequestInterceptor;

    public RestTemplatePostProcessor(MetricsClientHttpRequestInterceptor metricsClientHttpRequestInterceptor) {
        this.metricsClientHttpRequestInterceptor = metricsClientHttpRequestInterceptor;
    }

    public void customize(final AsyncRestTemplate restTemplate) {
        if (restTemplate.getInterceptors().contains(this.metricsClientHttpRequestInterceptor)) {
            return;
        }
        UriTemplateHandler templateHandler = restTemplate.getUriTemplateHandler();
        templateHandler = this.metricsClientHttpRequestInterceptor.createUriTemplateHandler(templateHandler);
        restTemplate.setUriTemplateHandler(templateHandler);
        List<AsyncClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(this.metricsClientHttpRequestInterceptor);
        interceptors.addAll(restTemplate.getInterceptors());
        restTemplate.setInterceptors(interceptors);
    }

    @Override
    public void customize(RestTemplate restTemplate) {
        if (restTemplate.getInterceptors().contains(this.metricsClientHttpRequestInterceptor)) {
            return;
        }
        UriTemplateHandler templateHandler = restTemplate.getUriTemplateHandler();
        templateHandler = this.metricsClientHttpRequestInterceptor.createUriTemplateHandler(templateHandler);
        restTemplate.setUriTemplateHandler(templateHandler);
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(this.metricsClientHttpRequestInterceptor);
        interceptors.addAll(restTemplate.getInterceptors());
        restTemplate.setInterceptors(interceptors);
    }
}
