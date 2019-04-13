package com.bkjk.platform.gray.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.RequestInterceptor;
import feign.RequestTemplate;

public class GrayPatternMatcherRequestInterceptor implements RequestInterceptor {
    public static final Logger logger = LoggerFactory.getLogger(GrayPatternMatcherRequestInterceptor.class);

    OpenFeignGrayHeaderCustomizer openFeignGrayHeaderCustomizer;

    public GrayPatternMatcherRequestInterceptor(OpenFeignGrayHeaderCustomizer openFeignGrayHeaderCustomizer) {
        this.openFeignGrayHeaderCustomizer = openFeignGrayHeaderCustomizer;
    }

    @Override
    public void apply(RequestTemplate template) {
        openFeignGrayHeaderCustomizer.apply(template);
    }
}
