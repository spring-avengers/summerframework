package com.bkjk.platform.webapi;

import com.bkjk.platform.webapi.misc.AutoRequestBodyProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class WebapiApplicationRunListener implements SpringApplicationRunListener {
    private static final Logger logger = LoggerFactory.getLogger(WebapiApplicationRunListener.class);
    public static final String PLATFORM_PROFILE_PRODUCT_NAME = "platform.profile.product.name";

    public WebapiApplicationRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void starting() {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        String profiles = environment.getProperty("spring.profiles.active");
        List<String> productProfiles = Arrays.asList("prod", "product");
        if (environment.containsProperty(PLATFORM_PROFILE_PRODUCT_NAME)) {
            productProfiles = Arrays.asList(environment.getProperty(PLATFORM_PROFILE_PRODUCT_NAME));
        }
        if (!StringUtils.isEmpty(profiles)) {
            if (productProfiles.stream().anyMatch(p -> profiles.toLowerCase().contains(p.toLowerCase()))) {
                environment.addActiveProfile("noSwagger");
            } else {
                environment.addActiveProfile("swagger");
            }
        }else {
            // 不指定profile则默认开启swagger，开发机一般不指定
            environment.addActiveProfile("swagger");
        }
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
        RequestMappingHandlerAdapter requestMappingHandlerAdapter = null;
        try {
            requestMappingHandlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);
        } catch (Throwable e) {
            // igore
        }
        if (!Objects.isNull(requestMappingHandlerAdapter)) {
            try {
                List<HandlerMethodReturnValueHandler> returnValueHandlers =
                    findHandlerMethodReturnValueHandlers(requestMappingHandlerAdapter);
                RequestResponseBodyMethodProcessor requestResponseBodyMethodProcessor =
                    findRequestResponseBodyMethodProcessor(returnValueHandlers);
                if (!Objects.isNull(requestResponseBodyMethodProcessor)) {
                    Field messageConvertersField =
                        ReflectionUtils.findField(RequestMappingHandlerAdapter.class, "messageConverters");
                    messageConvertersField.setAccessible(true);
                    List<HttpMessageConverter<?>> messageConverters = (List<HttpMessageConverter<?>>)ReflectionUtils
                        .getField(messageConvertersField, requestMappingHandlerAdapter);
                    Field contentNegotiationManagerField =
                        ReflectionUtils.findField(RequestMappingHandlerAdapter.class, "contentNegotiationManager");
                    contentNegotiationManagerField.setAccessible(true);
                    ContentNegotiationManager contentNegotiationManager = (ContentNegotiationManager)ReflectionUtils
                        .getField(contentNegotiationManagerField, requestMappingHandlerAdapter);
                    Field requestResponseBodyAdviceField =
                        ReflectionUtils.findField(RequestMappingHandlerAdapter.class, "requestResponseBodyAdvice");
                    requestResponseBodyAdviceField.setAccessible(true);
                    List<Object> requestResponseBodyAdvice = (List<Object>)ReflectionUtils
                        .getField(requestResponseBodyAdviceField, requestMappingHandlerAdapter);
                    int index = returnValueHandlers.indexOf(requestResponseBodyMethodProcessor);
                    returnValueHandlers.remove(requestResponseBodyMethodProcessor);
                    returnValueHandlers.add(index, new AutoRequestBodyProcessor(messageConverters,
                        contentNegotiationManager, requestResponseBodyAdvice));
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                logger.warn(e.getMessage());
            }
        }
    }

    /**
     * 反射获取HandlerMethodReturnValueHandler集合
     */
    private List<HandlerMethodReturnValueHandler>
        findHandlerMethodReturnValueHandlers(RequestMappingHandlerAdapter requestMappingHandlerAdapter)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field returnValueHandlerCompositeField =
            ReflectionUtils.findField(RequestMappingHandlerAdapter.class, "returnValueHandlers");
        returnValueHandlerCompositeField.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(returnValueHandlerCompositeField,
            returnValueHandlerCompositeField.getModifiers() & ~Modifier.FINAL);
        HandlerMethodReturnValueHandlerComposite returnValueHandlerComposite =
            (HandlerMethodReturnValueHandlerComposite)ReflectionUtils.getField(returnValueHandlerCompositeField,
                requestMappingHandlerAdapter);
        Field returnValueHandlersField =
            ReflectionUtils.findField(HandlerMethodReturnValueHandlerComposite.class, "returnValueHandlers");
        returnValueHandlersField.setAccessible(true);
        Field returnValueHandlerModifiersField = Field.class.getDeclaredField("modifiers");
        returnValueHandlerModifiersField.setAccessible(true);
        returnValueHandlerModifiersField.setInt(returnValueHandlersField,
            returnValueHandlersField.getModifiers() & ~Modifier.FINAL);
        List<HandlerMethodReturnValueHandler> returnValueHandlers =
            (List<HandlerMethodReturnValueHandler>)ReflectionUtils.getField(returnValueHandlersField,
                returnValueHandlerComposite);
        return returnValueHandlers;

    }

    private RequestResponseBodyMethodProcessor
        findRequestResponseBodyMethodProcessor(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        RequestResponseBodyMethodProcessor requestResponseBodyMethodProcessor = null;
        for (HandlerMethodReturnValueHandler returnValueHandler : returnValueHandlers) {
            if (returnValueHandler instanceof RequestResponseBodyMethodProcessor) {
                requestResponseBodyMethodProcessor = (RequestResponseBodyMethodProcessor)returnValueHandler;
                break;
            }
        }
        return requestResponseBodyMethodProcessor;
    }

    @Override
    public void running(ConfigurableApplicationContext context) {

    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
    }
}
