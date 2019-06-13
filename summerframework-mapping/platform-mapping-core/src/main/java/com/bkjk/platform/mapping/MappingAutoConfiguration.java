package com.bkjk.platform.mapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.mapping.config.MappingProperties;

import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

@Configuration
@ComponentScan("com.bkjk.platform.mapping")
@EnableConfigurationProperties(MappingProperties.class)
@Slf4j
public class MappingAutoConfiguration {

    @Autowired
    private MappingProperties properties;

    @ConditionalOnMissingBean
    @Bean
    public MapperFacade mapperFacade(MapperFactory mapperFactory) {
        return mapperFactory.getMapperFacade();
    }

    @ConditionalOnMissingBean
    @Bean
    public MapperFactory mapperFactory(DefaultMapperFactory.MapperFactoryBuilder<?, ?> mapperFactoryBuilder) {
        MapperFactory mapperFactory = mapperFactoryBuilder.build();
        return mapperFactory;
    }

    @ConditionalOnMissingBean
    @Bean
    public DefaultMapperFactory.MapperFactoryBuilder<?, ?> mapperFactoryBuilder() {
        DefaultMapperFactory.Builder builder = new DefaultMapperFactory.Builder();
        builder.useBuiltinConverters(properties.isUseBuiltinConverters());
        builder.useAutoMapping(properties.isUseAutoMapping());
        builder.mapNulls(properties.isMapNulls());
        builder.dumpStateOnException(properties.isDumpStateOnException());
        builder.favorExtension(properties.isFavorExtension());
        builder.captureFieldContext(properties.isCaptureFieldContext());
        log.debug("MappingProperties:" + properties.toString());
        return builder;
    }
}
