package com.bkjk.platform.gray;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.gray.feign.GrayPatternMatcherRequestInterceptor;
import com.bkjk.platform.gray.feign.OpenFeignGrayHeaderCustomizer;
import com.bkjk.platform.gray.httpclient.GrayHttpRequestInterceptor;
import com.bkjk.platform.gray.httpclient.HttpClientBuilderCustomizer;
import com.bkjk.platform.gray.httpclient.HttpClientGrayHeaderCustomizer;
import com.bkjk.platform.gray.resttemplate.GrayClientHttpRequestInterceptor;
import com.bkjk.platform.gray.resttemplate.RestTemplateGrayHeaderCustomizer;
import com.bkjk.platform.gray.resttemplate.RestTemplateInitializingBean;

@Configuration
@ConditionalOnBean(EurekaRegistration.class)
public class GrayAutoConfiguration {

    @Autowired
    private GrayRulesStore grayRulesStore;

    @Bean
    public GrayClientHttpRequestInterceptor
        grayClientHttpRequestInterceptor(RestTemplateGrayHeaderCustomizer restTemplateGrayHeaderCustomizer) {
        return new GrayClientHttpRequestInterceptor(restTemplateGrayHeaderCustomizer);
    }

    @Bean
    public GrayHttpRequestInterceptor
        grayHttpRequestInterceptor(HttpClientGrayHeaderCustomizer httpClientGrayHeaderCustomizer) {
        return new GrayHttpRequestInterceptor(httpClientGrayHeaderCustomizer);
    }

    @Bean
    public GrayPatternMatcherRequestInterceptor
        grayPatternMatcherRequestInterceptor(OpenFeignGrayHeaderCustomizer openFeignGrayHeaderCustomizer) {
        return new GrayPatternMatcherRequestInterceptor(openFeignGrayHeaderCustomizer);
    }

    @Bean
    public HttpClientBuilderCustomizer
        httpClientBuilderCustomizer(GrayHttpRequestInterceptor grayHttpRequestInterceptor) {
        return new HttpClientBuilderCustomizer(grayHttpRequestInterceptor);
    }

    @Bean
    public HttpClientGrayHeaderCustomizer httpClientGrayHeaderCustomizer(EurekaRegistration registration) {
        return new HttpClientGrayHeaderCustomizer(registration, grayRulesStore);
    }

    @Bean
    public OpenFeignGrayHeaderCustomizer openFeignGrayHeaderCustomizer(EurekaRegistration registration) {
        return new OpenFeignGrayHeaderCustomizer(registration, grayRulesStore);
    }

    @Bean
    public RestTemplateGrayHeaderCustomizer restTemplateGrayHeaderCustomizer(EurekaRegistration registration) {
        return new RestTemplateGrayHeaderCustomizer(registration, grayRulesStore);
    }

    @Bean
    public RestTemplateInitializingBean restTemplateInitializingBean() {
        return new RestTemplateInitializingBean();
    }

}
