package com.bkjk.platform.webapi.swagger;

import com.google.common.base.Predicates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.CorsFilter;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@EnableSwagger2
@Configuration
@Profile("swagger")
@Slf4j
public class SwaggerConfig implements ApplicationContextAware {
    @Autowired
    private ConfigurableEnvironment configurableEnvironment;
    private ApplicationContext applicationContext;

    @Value("${platform.swagger.basePackage:}")
    private String swaggerBasePackage;

    @Bean
    public Docket docket() {
        ArrayList<String> basePackage=new ArrayList<>();
        if(!StringUtils.isEmpty(swaggerBasePackage)){
            basePackage.addAll(Arrays.asList(swaggerBasePackage.split(",")));
        }else {
            // 从注解中解析basePackage
            Map<String, Object> applicationClass = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
            if(applicationClass.size()>1){
                log.warn("{} SpringBootApplication : {}",applicationClass.size(),applicationClass);
            }
            applicationClass.forEach((k,v)->{
                SpringBootApplication ann = AnnotationUtils.findAnnotation(v.getClass(), SpringBootApplication.class);
                if(ann.scanBasePackages().length==0&&ann.scanBasePackageClasses().length==0){
                    basePackage.add(v.getClass().getPackage().getName());
                }else {
                    basePackage.addAll(Arrays.asList(ann.scanBasePackages()));
                    basePackage.addAll(Arrays.asList(ann.scanBasePackageClasses()).stream().map(s->s.getPackage().getName()).collect(Collectors.toList()));
                }
            });
        }
        return new Docket(DocumentationType.SWAGGER_2).select().apis(Predicates.or(basePackage.stream().map(RequestHandlerSelectors::basePackage).collect(Collectors.toList())))
            .paths(Predicates.not(PathSelectors.regex("/error.*"))).build()
            .apiInfo(new ApiInfo(configurableEnvironment.getProperty("spring.application.name"), "",
                configurableEnvironment.getProperty("spring.application.version"), "",
                new Contact(configurableEnvironment.getProperty("spring.application.contact"), "", ""), "", "",
                new ArrayList<>()));
    }

    @Bean
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter swaggerCorsFilter() {
        return new SwaggerCorsFilter();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }
}
