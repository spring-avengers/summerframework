package com.bkjk.platform.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.toolkit.PackageHelper;
import com.bkjk.platform.mybatis.handler.SmartEnumTypeHandler;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasLength;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/4/9 15:50
 **/
public class CustomizeSqlSessionFactory {

    private  String typeEnumsPackage;
    private SqlSessionFactory sqlSessionFactory;
    private ApplicationContext applicationContext;

    public CustomizeSqlSessionFactory(String typeEnumsPackage,SqlSessionFactory sqlSessionFactory,ApplicationContext applicationContext) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.typeEnumsPackage=typeEnumsPackage;
        List<String> basePackage=new ArrayList<>();
        if(!StringUtils.isEmpty(this.typeEnumsPackage)){
            basePackage.addAll(Arrays.asList(this.typeEnumsPackage.split(",")));
        }else {
            // 从注解中解析basePackage
            Map<String, Object> applicationClass = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
            applicationClass.forEach((k,v)->{
                SpringBootApplication ann = AnnotationUtils.findAnnotation(v.getClass(), SpringBootApplication.class);
                if(ann.scanBasePackages().length==0&&ann.scanBasePackageClasses().length==0){
                    basePackage.add(v.getClass().getPackage().getName());
                }else {
                    basePackage.addAll(Arrays.asList(ann.scanBasePackages()));
                    basePackage.addAll(Arrays.asList(ann.scanBasePackageClasses()).stream().map(s->s.getPackage().getName()).collect(Collectors.toList()));
                }
            });
            List<String> newPackage = basePackage.stream().map(s -> s.endsWith("*") ? s : s + ".**").collect(Collectors.toList());
            basePackage.clear();
            basePackage.addAll(newPackage);
        }
        this.typeEnumsPackage=basePackage.stream().collect(Collectors.joining(","));
        if (hasLength(this.typeEnumsPackage)) {
            Set<Class> classes;
            if (typeEnumsPackage.contains(StringPool.STAR) && !typeEnumsPackage.contains(StringPool.COMMA)
                    && !typeEnumsPackage.contains(StringPool.SEMICOLON)) {
                classes = PackageHelper.scanTypePackage(typeEnumsPackage);
            } else {
                String[] typeEnumsPackageArray = tokenizeToStringArray(this.typeEnumsPackage,
                        ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
                Assert.notNull(typeEnumsPackageArray, "not find typeEnumsPackage:" + typeEnumsPackage);
                classes = new HashSet<>();
                for (String typePackage : typeEnumsPackageArray) {
                    Set<Class> scanTypePackage = PackageHelper.scanTypePackage(typePackage);
                    if (scanTypePackage.isEmpty()) {
                    } else {
                        classes.addAll(PackageHelper.scanTypePackage(typePackage));
                    }
                }
            }
            // 取得类型转换注册器
            TypeHandlerRegistry typeHandlerRegistry = sqlSessionFactory.getConfiguration().getTypeHandlerRegistry();
            for (Class cls : classes) {
                if (cls.isEnum()) {
                    // 原生方式
                    registerOriginalEnumTypeHandler(typeHandlerRegistry, cls);
                }
            }
        }
    }

    protected void registerOriginalEnumTypeHandler(TypeHandlerRegistry typeHandlerRegistry, Class<?> enumClazz) {
        typeHandlerRegistry.register(enumClazz, SmartEnumTypeHandler.class);
    }

}
