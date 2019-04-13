package com.bkjk.platform.openfeign;

import com.bkjk.platform.webapi.version.ApiVersionUtils;
import feign.*;
import io.swagger.annotations.ApiImplicitParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.openfeign.annotation.RequestParamParameterProcessor;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

public class OpenFeignSpringMvcContract extends OpenFeignBaseContract implements ResourceLoaderAware {

    public static final String CLASS_HEADER = "x-call-class";
    public static final String METHOD_HEADER = "x-call-method";

    public static class ConvertingExpander implements Param.Expander {

        private final ConversionService conversionService;

        public ConvertingExpander(ConversionService conversionService) {
            this.conversionService = conversionService;
        }

        @Override
        public String expand(Object value) {
            return this.conversionService.convert(value, String.class);
        }

    }

    protected class SimpleAnnotatedParameterContext implements AnnotatedParameterProcessor.AnnotatedParameterContext {

        private final MethodMetadata methodMetadata;

        private final int parameterIndex;

        public SimpleAnnotatedParameterContext(MethodMetadata methodMetadata, int parameterIndex) {
            this.methodMetadata = methodMetadata;
            this.parameterIndex = parameterIndex;
        }

        @Override
        public MethodMetadata getMethodMetadata() {
            return this.methodMetadata;
        }

        @Override
        public int getParameterIndex() {
            return this.parameterIndex;
        }

        @Override
        public void setParameterName(String name) {
            nameParam(this.methodMetadata, name, this.parameterIndex);
        }

        @Override
        public Collection<String> setTemplateParameter(String name, Collection<String> rest) {
            return addTemplatedParam(rest, name);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenFeignSpringMvcContract.class);

    private static final String ACCEPT = "Accept";

    private static final String CONTENT_TYPE = "Content-Type";

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private Pattern pattern = Pattern.compile("(\\{[^}]+\\})");

    private Field requestTemplateUrl = ReflectionUtils.findField(RequestTemplate.class, "url");
    {
        requestTemplateUrl.setAccessible(true);
    }

    private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;
    private final Map<String, Method> processedMethods = new HashMap<>();
    private final ConversionService conversionService;

    private final Param.Expander expander;

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    public OpenFeignSpringMvcContract() {
        this(Collections.emptyList());
    }

    public OpenFeignSpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
        this(annotatedParameterProcessors, new DefaultConversionService());
    }

    public OpenFeignSpringMvcContract(List<AnnotatedParameterProcessor> annotatedParameterProcessors,
        ConversionService conversionService) {
        Assert.notNull(annotatedParameterProcessors, "Parameter processors can not be null.");
        Assert.notNull(conversionService, "ConversionService can not be null.");

        List<AnnotatedParameterProcessor> processors;
        if (!annotatedParameterProcessors.isEmpty()) {
            processors = new ArrayList<>(annotatedParameterProcessors);
        } else {
            processors = getDefaultAnnotatedArgumentsProcessors();
        }
        this.annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
        this.conversionService = conversionService;
        this.expander = new SpringMvcContract.ConvertingExpander(conversionService);
    }

    protected void checkAtMostOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && (values.length == 0 || values.length == 1),
            "Method %s can only contain at most 1 %s field. Found: %s", method.getName(), fieldName,
            values == null ? null : Arrays.asList(values));
    }

    protected void checkOne(Method method, Object[] values, String fieldName) {
        checkState(values != null && values.length == 1, "Method %s can only contain 1 %s field. Found: %s",
            method.getName(), fieldName, values == null ? null : Arrays.asList(values));
    }

    private String defaultValue(Method method, String pathVariable) {
        Set<ApiImplicitParam> apiImplicitParams =
            AnnotatedElementUtils.findAllMergedAnnotations(method, ApiImplicitParam.class);
        for (ApiImplicitParam apiImplicitParam : apiImplicitParams) {
            if (pathVariable.equals(apiImplicitParam.name())) {
                return apiImplicitParam.allowableValues().split(",")[0].trim();
            }
        }
        throw new IllegalArgumentException("no default value for " + pathVariable);
    }

    protected List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {

        List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

        annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
        annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
        annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

        return annotatedArgumentResolvers;
    }

    private boolean hasPathVariable(MethodMetadata methodMetadata, String pathVariable) {
        for (Collection<String> names : methodMetadata.indexToName().values()) {
            if (names.contains(pathVariable)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
        this.processedMethods.put(Feign.configKey(targetType, method), method);
        MethodMetadata md = super.parseAndValidateMetadata(targetType, method);

        RequestMapping classAnnotation = findMergedAnnotation(targetType, RequestMapping.class);
        if (classAnnotation != null) {

            if (!md.template().headers().containsKey(ACCEPT)) {
                parseProduces(md, method, classAnnotation);
            }

            if (!md.template().headers().containsKey(CONTENT_TYPE)) {
                parseConsumes(md, method, classAnnotation);
            }

            parseHeaders(md, method, classAnnotation);
        }

        String rawUrl = md.template().url();
        String url = rawUrl;
        List<String> pathVariableList = new ArrayList<>();

        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            String pathVariable = matcher.group();
            int endIndex = pathVariable.indexOf(":");
            if (endIndex != -1) {
                String rawPathVariable = pathVariable.substring(1, endIndex);
                pathVariableList.add(rawPathVariable);
                url = url.replace(pathVariable, "{" + rawPathVariable + "}");
            } else {
                String rawPathVariable = pathVariable.substring(1, pathVariable.length() - 1);
                pathVariableList.add(rawPathVariable);
            }
        }

        for (String pathVariable : pathVariableList) {
            if (!hasPathVariable(md, pathVariable)) {
                url = url.replace("{" + pathVariable + "}", defaultValue(method, pathVariable));
                ReflectionUtils.setField(requestTemplateUrl, md.template(), new StringBuilder(url));
            }
        }

        String apiVersion = ApiVersionUtils.getFirstValue(targetType, method);
        if (!StringUtils.isEmpty(apiVersion)) {
            md.template().insert(0, "/" + apiVersion);
        }
        LOGGER.info("{} > {}", rawUrl, url);
        md.template().header(CLASS_HEADER, targetType.getSimpleName()).header(METHOD_HEADER, method.getName());
        return md;
    }

    protected void parseConsumes(MethodMetadata md, Method method, RequestMapping annotation) {
        String[] serverConsumes = annotation.consumes();
        String clientProduces = serverConsumes.length == 0 ? null : emptyToNull(serverConsumes[0]);
        if (clientProduces != null) {
            md.template().header(CONTENT_TYPE, clientProduces);
        }
    }

    protected void parseHeaders(MethodMetadata md, Method method, RequestMapping annotation) {

        if (annotation.headers() != null && annotation.headers().length > 0) {
            for (String header : annotation.headers()) {
                int index = header.indexOf('=');
                if (!header.contains("!=") && index >= 0) {
                    md.template().header(resolve(header.substring(0, index)),
                        resolve(header.substring(index + 1).trim()));
                }
            }
        }
    }

    protected void parseProduces(MethodMetadata md, Method method, RequestMapping annotation) {
        String[] serverProduces = annotation.produces();
        String clientAccepts = serverProduces.length == 0 ? null : emptyToNull(serverProduces[0]);
        if (clientAccepts != null) {
            md.template().header(ACCEPT, clientAccepts);
        }
    }

    @Override
    protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
        if (clz.getInterfaces().length == 0) {
            RequestMapping classAnnotation = findMergedAnnotation(clz, RequestMapping.class);
            if (classAnnotation != null) {

                if (classAnnotation.value().length > 0) {
                    String pathValue = emptyToNull(classAnnotation.value()[0]);
                    pathValue = resolve(pathValue);
                    if (!pathValue.startsWith("/")) {
                        pathValue = "/" + pathValue;
                    }
                    data.template().insert(0, pathValue);
                }
            }
        }
    }

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data, Annotation methodAnnotation, Method method) {
        if (!RequestMapping.class.isInstance(methodAnnotation)
            && !methodAnnotation.annotationType().isAnnotationPresent(RequestMapping.class)) {
            return;
        }

        RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);

        RequestMethod[] methods = methodMapping.method();
        if (methods.length == 0) {
            methods = new RequestMethod[] {RequestMethod.GET};
        }
        checkOne(method, methods, "method");
        data.template().method(methods[0].name());

        checkAtMostOne(method, methodMapping.value(), "value");
        if (methodMapping.value().length > 0) {
            String pathValue = emptyToNull(methodMapping.value()[0]);
            if (pathValue != null) {
                pathValue = resolve(pathValue);

                if (!pathValue.startsWith("/") && !data.template().toString().endsWith("/")) {
                    pathValue = "/" + pathValue;
                }
                data.template().append(pathValue);
            }
        }

        parseProduces(data, method, methodMapping);

        parseConsumes(data, method, methodMapping);

        parseHeaders(data, method, methodMapping);

        data.indexToExpander(new LinkedHashMap<Integer, Param.Expander>());
    }

    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] annotations, int paramIndex) {
        boolean isHttpAnnotation = false;

        AnnotatedParameterProcessor.AnnotatedParameterContext context =
            new OpenFeignSpringMvcContract.SimpleAnnotatedParameterContext(data, paramIndex);
        Method method = this.processedMethods.get(data.configKey());
        for (Annotation parameterAnnotation : annotations) {
            AnnotatedParameterProcessor processor =
                this.annotatedArgumentProcessors.get(parameterAnnotation.annotationType());
            if (processor != null) {
                Annotation processParameterAnnotation;

                processParameterAnnotation =
                    synthesizeWithMethodParameterNameAsFallbackValue(parameterAnnotation, method, paramIndex);
                isHttpAnnotation |= processor.processArgument(context, processParameterAnnotation, method);
            }
        }
        if (isHttpAnnotation && data.indexToExpander().get(paramIndex) == null
            && this.conversionService.canConvert(method.getParameterTypes()[paramIndex], String.class)) {
            data.indexToExpander().put(paramIndex, this.expander);
        }
        return isHttpAnnotation;
    }

    protected String resolve(String value) {
        if (StringUtils.hasText(value) && this.resourceLoader instanceof ConfigurableApplicationContext) {
            return ((ConfigurableApplicationContext)this.resourceLoader).getEnvironment().resolvePlaceholders(value);
        }
        return value;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    protected boolean shouldAddParameterName(int parameterIndex, Type[] parameterTypes, String[] parameterNames) {

        return parameterNames != null && parameterNames.length > parameterIndex

            && parameterTypes != null && parameterTypes.length > parameterIndex;
    }

    protected Annotation synthesizeWithMethodParameterNameAsFallbackValue(Annotation parameterAnnotation, Method method,
        int parameterIndex) {
        Map<String, Object> annotationAttributes = AnnotationUtils.getAnnotationAttributes(parameterAnnotation);
        Object defaultValue = AnnotationUtils.getDefaultValue(parameterAnnotation);
        if (defaultValue instanceof String && defaultValue.equals(annotationAttributes.get(AnnotationUtils.VALUE))) {
            Type[] parameterTypes = method.getGenericParameterTypes();
            String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
            if (shouldAddParameterName(parameterIndex, parameterTypes, parameterNames)) {
                annotationAttributes.put(AnnotationUtils.VALUE, parameterNames[parameterIndex]);
            }
        }
        return AnnotationUtils.synthesizeAnnotation(annotationAttributes, parameterAnnotation.annotationType(), null);
    }

    protected Map<Class<? extends Annotation>, AnnotatedParameterProcessor>
        toAnnotatedArgumentProcessorMap(List<AnnotatedParameterProcessor> processors) {
        Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
        for (AnnotatedParameterProcessor processor : processors) {
            result.put(processor.getAnnotationType(), processor);
        }
        return result;
    }
}
