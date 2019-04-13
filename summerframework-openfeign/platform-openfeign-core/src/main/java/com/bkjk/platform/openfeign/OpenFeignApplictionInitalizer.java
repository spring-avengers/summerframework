package com.bkjk.platform.openfeign;

import java.lang.reflect.Field;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;

import com.bkjk.platform.openfeign.decoder.ApiResultDecoder;
import com.bkjk.platform.openfeign.decoder.ApiResultHandler;
import com.bkjk.platform.openfeign.decoder.Summer2ErrorApiResultHandler;
import com.bkjk.platform.openfeign.decoder.SummerErrorAdapterApiResultHandler;

import feign.codec.Decoder;

public class OpenFeignApplictionInitalizer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().registerSingleton("summerErrorAdapterApiResultHandler",
            new SummerErrorAdapterApiResultHandler());
        applicationContext.getBeanFactory().registerSingleton("summer2ErrorApiResultHandler",
            new Summer2ErrorApiResultHandler());
        applicationContext.getBeanFactory().addBeanPostProcessor(new InstantiationAwareBeanPostProcessorAdapter() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof Decoder && !(bean instanceof ApiResultDecoder)) {
                    Decoder decode = (Decoder)bean;
                    return new ApiResultDecoder(decode, applicationContext.getBeansOfType(ApiResultHandler.class)
                        .values().stream().collect(Collectors.toList()));
                } else if (bean instanceof RequestParamMethodArgumentResolver) {
                    RequestParamMethodArgumentResolver requestParamMethodArgumentResolver =
                        (RequestParamMethodArgumentResolver)bean;
                    Field useDefaultResolution = ReflectionUtils.findField(RequestParamMethodArgumentResolver.class,
                        "useDefaultResolution", RequestParamMethodArgumentResolver.class);
                    ReflectionUtils.makeAccessible(useDefaultResolution);
                    Boolean useDefaultResolutionValue =
                        (Boolean)ReflectionUtils.getField(useDefaultResolution, requestParamMethodArgumentResolver);
                    return new RequestParamMethodArgumentResolverExt(useDefaultResolutionValue);
                } else {
                    return bean;
                }

            }
        });

    }

    public static class RequestParamMethodArgumentResolverExt extends RequestParamMethodArgumentResolver {

        public RequestParamMethodArgumentResolverExt(boolean useDefaultResolution) {
            super(useDefaultResolution);
        }

        @Override
        protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
            RequestParam ann = parameter.getParameterAnnotation(RequestParam.class);
            return (ann != null ? new RequestParamNamedValueInfo(ann) : new RequestParamNamedValueInfo());
        }

        private static class RequestParamNamedValueInfo extends NamedValueInfo {

            public RequestParamNamedValueInfo() {
                super("", false, ValueConstants.DEFAULT_NONE);
            }

            public RequestParamNamedValueInfo(RequestParam annotation) {
                super((annotation.name() != null && annotation.name() != "") ? annotation.name() : annotation.value(),
                    annotation.required(), annotation.defaultValue());

            }
        }

    }

}
