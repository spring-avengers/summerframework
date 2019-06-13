package com.bkjk.platform.mapping.converters;

import java.util.Arrays;

import org.springframework.util.StringUtils;

import com.bkjk.platform.mapping.util.Expression;
import com.bkjk.platform.mapping.util.ExpressionUtil;

import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.metadata.Type;

public class EnumAndNumberConverter<T extends Enum<T>> extends BidirectionalConverter<T, Number> {
    private String methodName;

    public EnumAndNumberConverter(Expression<T, Number> column) {
        this.methodName = ExpressionUtil.expressionToMethodName(column);
    }

    @Override
    public T convertFrom(Number o, Type<T> type, MappingContext mappingContext) {
        if (StringUtils.isEmpty(methodName)) {
            return Arrays.stream(type.getRawType().getEnumConstants()).filter(item -> item.name().equals(o)).findFirst()
                .orElse(null);
        } else {
            return Arrays.stream(type.getRawType().getEnumConstants()).filter(item -> {
                try {
                    return type.getRawType().getDeclaredMethod(methodName).invoke(item).equals(o);
                } catch (Exception e) {
                }
                return false;
            }).findFirst().orElse(null);
        }
    }

    @Override
    public Number convertTo(T t, Type<Number> type, MappingContext mappingContext) {
        if (StringUtils.isEmpty(methodName)) {
            return t.ordinal();
        } else {
            try {
                return (Number)t.getClass().getMethod(methodName).invoke(t);
            } catch (Exception e) {
                return null;
            }
        }
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
        EnumAndNumberConverter other = (EnumAndNumberConverter)obj;
        if (methodName == null) {
            if (other.methodName != null) {
                return false;
            }
        } else if (!methodName.equals(other.methodName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        return result;
    }
}
