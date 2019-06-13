package com.bkjk.platform.mapping.test;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.bkjk.platform.mapping.config.FieldMapper;
import com.bkjk.platform.mapping.config.FieldMapperDirection;
import com.bkjk.platform.mapping.config.MappingRegister;
import com.bkjk.platform.mapping.converters.EnumAndNumberConverter;
import com.bkjk.platform.mapping.converters.EnumAndStringConverter;
import com.bkjk.platform.mapping.converters.LocalDateAndStringConverter;
import com.bkjk.platform.mapping.converters.LocalDateTimeAndStringConverter;
import com.bkjk.platform.mapping.converters.LocalTimeAndStringConverter;

@Configuration
public class MappingConfig {
    @Autowired
    MappingRegister mappingRegister;

    @PostConstruct
    public void config() {
        mappingRegister.registerConverter("intEnumConverter", new EnumAndNumberConverter<>(IntEnum::getCode))
            .registerConverter("simpleEnumConverter", new EnumAndStringConverter<>(null))
            .registerConverter("stringEnumConverter", new EnumAndStringConverter<>(StringEnum::getInfo))
            .registerConverter("threeFieldsEnumConverter", new EnumAndStringConverter<>(ThreeFieldsEnum::getKey))
            .registerMapper(EnumTest1.class, EnumTest2.class,
                new FieldMapper<>(EnumTest1::getIntEnum, "intEnumConverter"),
                new FieldMapper<>(EnumTest1::getSimpleEnum, "simpleEnumConverter"),
                new FieldMapper<>(EnumTest1::getStringEnum, "stringEnumConverter"),
                new FieldMapper<>(EnumTest1::getThreeFieldsEnum, "threeFieldsEnumConverter"));

        mappingRegister
            .registerConverter("localDateTimeAndStringConverter",
                new LocalDateTimeAndStringConverter("yyyy-MM-dd HH:mm:ss.SSS"))
            .registerConverter("localDateAndStringConverter", new LocalDateAndStringConverter("yyyy-MM-dd"))
            .registerConverter("localTimeAndStringConverter", new LocalTimeAndStringConverter("HH:mm:ss"))
            .registerMapper(DateTest1.class, DateTest2.class,
                new FieldMapper<>(DateTest1::getDate, DateTest2::getLocalDate, "localDateAndStringConverter"),
                new FieldMapper<>(DateTest1::getTime, DateTest2::getLocalTime, "localTimeAndStringConverter"),
                new FieldMapper<>(DateTest1::getDateTime, DateTest2::getLocalDateTime,
                    "localDateTimeAndStringConverter"));

        mappingRegister.registerMapper(SkipTest1.class, SkipTest2.class,
            new FieldMapper<>(SkipTest1::getTest, SkipTest2::getTestFoo),
            new FieldMapper<>(SkipTest1::getAa, SkipTest2::getAaa, true, FieldMapperDirection.A_TO_B),
            new FieldMapper<>(SkipTest1::getAa, SkipTest2::getAaa, false, FieldMapperDirection.B_TO_A),
            new FieldMapper<>(SkipTest1::getBb, true));
    }
}
