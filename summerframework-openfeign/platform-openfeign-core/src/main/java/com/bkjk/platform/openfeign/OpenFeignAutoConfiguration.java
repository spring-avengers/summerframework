
package com.bkjk.platform.openfeign;

import com.bkjk.platform.openfeign.decoder.ApiErrorDecoder;
import com.bkjk.platform.openfeign.decoder.ApiResultDecoder;
import com.bkjk.platform.openfeign.decoder.ApiResultHandler;
import com.bkjk.platform.openfeign.decoder.SummerWrappedApiResultHandler;
import com.bkjk.platform.webapi.result.ApiResultTransformer;
import com.bkjk.platform.webapi.result.ApiResultWrapper;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnClass(Feign.class)
public class OpenFeignAutoConfiguration {

    public static MethodParameter interfaceMethodParameter(MethodParameter parameter, Class annotationType) {
        if (!parameter.hasParameterAnnotation(annotationType)) {
            for (Class<?> itf : parameter.getDeclaringClass().getInterfaces()) {
                try {
                    Method method =
                        itf.getMethod(parameter.getMethod().getName(), parameter.getMethod().getParameterTypes());
                    MethodParameter itfParameter = new InterfaceMethodParameter(method, parameter.getParameterIndex());
                    if (itfParameter.hasParameterAnnotation(annotationType)) {
                        return itfParameter;
                    }
                } catch (NoSuchMethodException e) {
                    continue;
                }
            }
        }
        return parameter;
    }

    private static class InterfaceMethodParameter extends SynthesizingMethodParameter {

        private volatile Annotation[] parameterAnnotations;

        private Method interfaceMethod;

        public InterfaceMethodParameter(Method interfaceMethod, int parameterIndex) {
            super(interfaceMethod, parameterIndex);
            this.interfaceMethod = interfaceMethod;
        }

        @Override
        public Annotation[] getParameterAnnotations() {
            Annotation[][] annotationArray = this.interfaceMethod.getParameterAnnotations();
            if (this.getParameterIndex() >= 0 && this.getParameterIndex() < annotationArray.length) {
                this.parameterAnnotations = annotationArray[this.getParameterIndex()];
                for (int i = 0; i < this.parameterAnnotations.length; i++) {
                    this.parameterAnnotations[i] =
                        AnnotationUtils.synthesizeAnnotation(this.parameterAnnotations[i], null);
                }
            } else {
                this.parameterAnnotations = new Annotation[0];
            }
            return this.parameterAnnotations;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

    }

    @Autowired
    private RequestMappingHandlerAdapter adapter;

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @Bean
    public OpenFeignRequestInterceptor feignRequestInterceptor() {
        return new OpenFeignRequestInterceptor();
    }

    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

    @Configuration
    @ConditionalOnClass(ApiResultWrapper.class)
    static class ApiVersionConfig {

        @Autowired
        private ObjectFactory<HttpMessageConverters> messageConverters;

        @Bean
        public SummerWrappedApiResultHandler
            summer2WrappedApiResultHandler(ApiResultTransformer<? extends ApiResultWrapper> apiResultTransformer) {
            if (apiResultTransformer.getType().isInterface()) {
                throw new IllegalArgumentException(
                    "ApiResultTransformer.getType() should be type of any ***class*** that implements ApiResultWrapper");
            }
            return new SummerWrappedApiResultHandler(apiResultTransformer.getType(),
                new SpringDecoder(this.messageConverters));
        }

    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder feignErrorDecoder() {
        return new ApiErrorDecoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public Decoder feignDecoder(List<ApiResultHandler> apiResultHandlers) {
        return new ApiResultDecoder(new ResponseEntityDecoder(new SpringDecoder(this.messageConverters)),
            apiResultHandlers);
    }

    @Bean
    public OpenFeignSpringMvcContract feignSpringMvcContract(
        @Autowired(required = false) List<AnnotatedParameterProcessor> parameterProcessors,
        List<ConversionService> conversionServices) {
        if (conversionServices.size() == 0) {
            throw new IllegalStateException("ConversionService can not be NULL");
        }
        ConversionService conversionService = null;
        if (conversionServices.size() == 1) {
            conversionService = conversionServices.get(0);
        } else {
            // 如果有多个实例，优先使用找到的第一个DefaultFormattingConversionService，如果没有，则使用FormattingConversionService
            conversionService = conversionServices.stream().filter(c -> c instanceof DefaultFormattingConversionService)
                .findFirst().orElseGet(() -> conversionServices.stream()
                    .filter(c -> c instanceof FormattingConversionService).findFirst().get());
        }
        if (null == parameterProcessors) {
            parameterProcessors = new ArrayList<>();
        }
        return new OpenFeignSpringMvcContract(parameterProcessors, conversionService);
    }

    @PostConstruct
    public void modifyArgumentResolvers() {
        List<HandlerMethodArgumentResolver> list = new ArrayList<>(adapter.getArgumentResolvers());

        list.add(0, new RequestParamMethodArgumentResolver(true) {
            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, RequestParam.class));
            }

            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, RequestParam.class));
            }
        });

        list.add(0, new PathVariableMethodArgumentResolver() {
            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, PathVariable.class));
            }

            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, PathVariable.class));
            }
        });

        list.add(0, new RequestHeaderMethodArgumentResolver(beanFactory) {
            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, RequestHeader.class));
            }

            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, RequestHeader.class));
            }
        });

        list.add(0, new ServletCookieValueMethodArgumentResolver(beanFactory) {
            @Override
            protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
                return super.createNamedValueInfo(interfaceMethodParameter(parameter, CookieValue.class));
            }

            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, CookieValue.class));
            }
        });

        list.add(0, new RequestResponseBodyMethodProcessor(adapter.getMessageConverters()) {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return super.supportsParameter(interfaceMethodParameter(parameter, RequestBody.class));
            }

            @Override
            protected void validateIfApplicable(WebDataBinder binder, MethodParameter methodParam) {
                super.validateIfApplicable(binder, interfaceMethodParameter(methodParam, Valid.class));
            }
        });

        List<HandlerMethodArgumentResolver> customResolvers =
                adapter.getCustomArgumentResolvers ();
        list.removeAll (customResolvers);
        list.addAll (0, customResolvers);
        adapter.setArgumentResolvers (list);
    }
}
