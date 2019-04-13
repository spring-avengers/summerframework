package com.bkjk.platform.gray.resttemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

public class RestTemplateInitializingBean implements InitializingBean {
    @Autowired(required = false)
    @Lazy
    private List<RestTemplate> restTemplateList;

    @Autowired
    GrayClientHttpRequestInterceptor grayClientHttpRequestInterceptor;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (null != restTemplateList) {
            for (RestTemplate restTemplate : restTemplateList) {
                List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                if (interceptors == null) {
                    restTemplate.setInterceptors(Arrays.asList(grayClientHttpRequestInterceptor));
                } else {
                    List<ClientHttpRequestInterceptor> newInterceptors = new ArrayList<>();
                    newInterceptors.addAll(interceptors);
                    newInterceptors.add(grayClientHttpRequestInterceptor);
                    restTemplate.setInterceptors(newInterceptors);
                }
            }
        }
    }
}
