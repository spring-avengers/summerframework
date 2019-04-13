package com.bkjk.platform.gray;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

@Configuration
public class GrayFilterAutoConfiguration {

    public static final String METADATA_GRAY_ENABLE = "gray.enable";
    public static final String METADATA_GRAY_ENABLE_TRUE = "true";

    public static final String BKJKGRAY = "bkjkgray";

    @Value("${" + BKJKGRAY + ":}")
    private String bkjkgray;

    @Bean
    public FilterRegistrationBean cacheContentFilterBean() {
        FilterRegistrationBean ret = new FilterRegistrationBean();
        ret.setFilter(requestBodyCacheFilter());
        ret.addUrlPatterns("/*");
        ret.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return ret;
    }

    @Bean("grayScriptEngine")
    @Primary
    public GrayScriptEngine grayScriptEngine(GroovyGrayScriptEngine groovyGrayScriptEngine) {
        return new CacheableGrayScriptEngine(groovyGrayScriptEngine);
    }

    @Bean
    public GrayServerFilter grayServerFilter(final EurekaRegistration registration) {

        registration.getMetadata().put(METADATA_GRAY_ENABLE, METADATA_GRAY_ENABLE_TRUE);
        String bkjkgrayToUse = StringUtils.isEmpty(bkjkgray) ? System.getProperty(BKJKGRAY) : bkjkgray;
        if (!StringUtils.isEmpty(bkjkgrayToUse)) {
            registration.getMetadata().put(BKJKGRAY, bkjkgrayToUse);
        }
        return new GrayServerFilter();
    }

    @Bean("groovyGrayScriptEngine")
    public GroovyGrayScriptEngine groovyGrayScriptEngine() {
        return new GroovyGrayScriptEngine();
    }

    @Bean
    public RequestBodyCacheFilter requestBodyCacheFilter() {
        return new RequestBodyCacheFilter();
    }
}
