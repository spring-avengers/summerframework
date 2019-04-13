package com.bkjk.platform.webapi;

import com.bkjk.platform.webapi.filter.ApiFilterInterceptor;
import com.bkjk.platform.webapi.filter.ApiResponseFilter;
import com.bkjk.platform.webapi.misc.AutoRequestBodyProcessor;
import com.bkjk.platform.webapi.result.ApiResultTransformer;
import com.bkjk.platform.webapi.result.DefaultApiResultTransformer;
import com.bkjk.platform.webapi.result.StringOrJsonHttpMessageConverter;
import com.bkjk.platform.webapi.version.ApiVersionHandlerMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Iterator;
import java.util.List;

@Configuration
@ComponentScan(value = "com.bkjk.platform.webapi",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.bkjk.platform.webapi.test.*"))
public class WebApiConfig implements WebMvcConfigurer, WebMvcRegistrations {

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Autowired
    List<HttpMessageConverter<?>> httpMessageConverters;

    @Autowired
    ApiFilterInterceptor apiFilterInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiFilterInterceptor).addPathPatterns("/**").excludePathPatterns("/favicon.ico");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Iterator<HttpMessageConverter<?>> it = converters.iterator();
        List<HttpMessageConverter<?>> customeCovertList = Lists.newArrayList();
        while (it.hasNext()) {
            HttpMessageConverter<?> converter = it.next();
            if (!StringUtils.startsWithIgnoreCase(converter.getClass().getName(), "org.springframework")) {
                customeCovertList.add(converter);
            }
        }
        converters.removeAll(customeCovertList);
        StringOrJsonHttpMessageConverter converter;
        if(objectMapper==null){
            converter=new StringOrJsonHttpMessageConverter();
        }else {
            converter=new StringOrJsonHttpMessageConverter(objectMapper);
        }
        if (!customeCovertList.isEmpty()) {
            converters.addAll(0, customeCovertList);
            converters.add(customeCovertList.size(), converter);
        } else {
            converters.add(0, converter);
        }
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new ApiVersionHandlerMapping();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AutoRequestBodyProcessor(httpMessageConverters));
    }

    @Bean
    @ConditionalOnMissingBean(ApiResultTransformer.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ApiResultTransformer apiResultTransformer() {
        return new DefaultApiResultTransformer();
    }

    @Bean
    public ApiResponseFilter apiResponseFilter() {
        return new ApiResponseFilter.Default();
    }

}
