package com.bkjk.platform.webapi.version;

import com.bkjk.platform.webapi.ApiController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
public class ApiVersionHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected boolean isHandler(Class<?> beanType) {
        return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
    }

    @Override
    protected HandlerMethod createHandlerMethod(Object handler, Method method) {
        HandlerMethod handlerMethod;
        if (handler instanceof String) {
            String beanName = (String)handler;
            handlerMethod = new RemoteServiceHandlerMethod(beanName,
                    getApplicationContext().getAutowireCapableBeanFactory(), method);
        } else {
            handlerMethod = new RemoteServiceHandlerMethod(handler, method);
        }
        return handlerMethod;
    }

    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        Class<?> controllerClass = method.getDeclaringClass();

        if (AnnotatedElementUtils.hasAnnotation(controllerClass, ApiController.class)) {

            ApiVersion apiVersion = AnnotationUtils.findAnnotation(controllerClass, ApiVersion.class);

            ApiVersion methodAnnotation = AnnotationUtils.findAnnotation(method, ApiVersion.class);
            if (methodAnnotation != null) {
                apiVersion = methodAnnotation;
            }

            String[] urlPatterns = apiVersion == null ? new String[0] : apiVersion.value();

            PatternsRequestCondition apiPattern = new PatternsRequestCondition(urlPatterns);
            PatternsRequestCondition oldPattern = mapping.getPatternsCondition();
            PatternsRequestCondition updatedFinalPattern = apiPattern.combine(oldPattern);
            mapping = new RequestMappingInfo(mapping.getName(), updatedFinalPattern, mapping.getMethodsCondition(),
                mapping.getParamsCondition(), mapping.getHeadersCondition(), mapping.getConsumesCondition(),
                mapping.getProducesCondition(), mapping.getCustomCondition());
        }

        super.registerHandlerMethod(handler, method, mapping);
    }


    private static class RemoteServiceHandlerMethod extends HandlerMethod {

        private Method interfaceMethod;

        private Class<?> interfaceClass;

        public RemoteServiceHandlerMethod(Object bean, Method method) {
            super(bean, method);
            changeType();
        }

        public RemoteServiceHandlerMethod(Object bean, String methodName, Class<?>... parameterTypes)
                throws NoSuchMethodException {
            super(bean, methodName, parameterTypes);
            changeType();
        }

        public RemoteServiceHandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
            super(beanName, beanFactory, method);
            changeType();
        }

        private void changeType() {
            // 兼容SpringMVC,Controller不需要接口,直接实现的方式;
            this.interfaceClass = getApiInterface(getMethod().getDeclaringClass());
            try {
                interfaceMethod = interfaceClass.getMethod(getMethod().getName(), getMethod().getParameterTypes());
                MethodParameter[] params = super.getMethodParameters();
                for (int i = 0; i < params.length; i++) {
                    params[i] = new RemoteServiceMethodParameter(params[i]);
                }
            } catch (NoSuchMethodException | SecurityException e) {
                log.info("Cant find method {} in interface {}",getMethod().getName(),interfaceClass);
            }
        }

        private class RemoteServiceMethodParameter extends MethodParameter {

            private volatile Annotation[] parameterAnnotations;

            public RemoteServiceMethodParameter(MethodParameter methodParameter) {
                super(methodParameter);
            }

            @Override
            public Annotation[] getParameterAnnotations() {
                if (Objects.isNull(this.parameterAnnotations)) {
                    if (Objects.nonNull(RemoteServiceHandlerMethod.this.interfaceMethod)) {
                        Annotation[][] annotationArray =
                                RemoteServiceHandlerMethod.this.interfaceMethod.getParameterAnnotations();
                        setParameterAnnotations(annotationArray);
                    } else {
                        this.parameterAnnotations = super.getParameterAnnotations();
                    }
                }
                return this.parameterAnnotations;
            }

            private void setParameterAnnotations(Annotation[][] annotationArray) {
                if (this.getParameterIndex() >= 0 && this.getParameterIndex() < annotationArray.length) {
                    this.parameterAnnotations = annotationArray[this.getParameterIndex()];
                } else {
                    this.parameterAnnotations = new Annotation[0];
                }
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

        @Override
        public boolean equals(Object other) {
            return super.equals(other);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

    }

    public static final String FEIGN_CLIENT_ANNOTATION="org.springframework.cloud.openfeign.FeignClient";
    private static Class<? extends Annotation> FEIGN_CLIENT_CLASS;
    static {
        try {
            FEIGN_CLIENT_CLASS = (Class<? extends Annotation>) ClassUtils.forName(FEIGN_CLIENT_ANNOTATION,ApiVersionHandlerMapping.class.getClassLoader());
        } catch (ClassNotFoundException ignore) {
        }
    }
    private static Class<?> getApiInterface(Class<?> beanType) {
        if(FEIGN_CLIENT_CLASS ==null){
            return beanType;
        }
        // 递归查找父接口中的包含 FeignClient 注解的接口，这个接口中包含所有mapping注解
        Class<?> found = findFirstInterface(beanType, FEIGN_CLIENT_CLASS);
        return found==null?beanType:found;
    }

    private static Class<?> findFirstInterface(Class<?> beanType, Class<? extends Annotation> annotationClass) {
        if (beanType.isAnnotationPresent(annotationClass)) {
            return beanType;
        }
        final Class<?>[] interfaces = beanType.getInterfaces();
        for (Class<?> currentInterface : interfaces){
            Class<?> found = findFirstInterface(currentInterface, annotationClass);
            if(found!=null){
                return found;
            }
        }
        return null;
    }


}
