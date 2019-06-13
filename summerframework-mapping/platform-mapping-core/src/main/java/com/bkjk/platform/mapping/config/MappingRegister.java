package com.bkjk.platform.mapping.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ma.glasnost.orika.Converter;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import ma.glasnost.orika.metadata.FieldMapBuilder;

@Component
public class MappingRegister {

    @Autowired
    private MapperFactory factory;

    public <A, B> MappingRegister registerConverter(Converter<A, B> converter) {
        factory.getConverterFactory().registerConverter(converter);
        return this;
    }

    public <A, B> MappingRegister registerConverter(String converterId, Converter<A, B> converter) {
        factory.getConverterFactory().registerConverter(converterId, converter);
        return this;
    }

    public <A, B> MappingRegister registerMapper(Class<A> aType, Class<B> bType, FieldMapper<A, B>... fieldMappers) {
        ClassMapBuilder classMapBuilder = factory.classMap(aType, bType);
        for (FieldMapper fieldMapper : fieldMappers) {
            FieldMapBuilder fieldMapBuilder =
                classMapBuilder.fieldMap(fieldMapper.getFieldOfA(), fieldMapper.getFieldOfB());
            switch (fieldMapper.getDirection()) {
                case A_TO_B:
                    fieldMapBuilder.aToB();
                    break;
                case B_TO_A:
                    fieldMapBuilder.bToA();
                    break;
                default:
                    break;
            }

            if (fieldMapper.isSkip())
                fieldMapBuilder.exclude();
            if (!StringUtils.isEmpty(fieldMapper.getConverter()))
                fieldMapBuilder.converter(fieldMapper.getConverter());
            fieldMapBuilder.add();
        }
        classMapBuilder.byDefault().register();
        return this;
    }
}
