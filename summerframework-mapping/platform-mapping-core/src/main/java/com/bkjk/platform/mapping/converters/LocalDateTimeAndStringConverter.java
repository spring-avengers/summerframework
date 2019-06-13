package com.bkjk.platform.mapping.converters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

public class LocalDateTimeAndStringConverter extends BidirectionalConverter<LocalDateTime, String> {

    private DateTimeFormatter dateTimeFormatter;

    public LocalDateTimeAndStringConverter(String pattern) {
        dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalDateTime convertFrom(String source, Type<LocalDateTime> destinationType,
        MappingContext mappingContext) {
        return LocalDateTime.parse(source, dateTimeFormatter);
    }

    @Override
    public String convertTo(LocalDateTime source, Type<String> destinationType, MappingContext mappingContext) {
        return dateTimeFormatter.format(source);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LocalDateTimeAndStringConverter other = (LocalDateTimeAndStringConverter)obj;
        if (dateTimeFormatter == null) {
            if (other.dateTimeFormatter != null) {
                return false;
            }
        } else if (!dateTimeFormatter.equals(other.dateTimeFormatter)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((dateTimeFormatter == null) ? 0 : dateTimeFormatter.hashCode());
        return result;
    }
}
