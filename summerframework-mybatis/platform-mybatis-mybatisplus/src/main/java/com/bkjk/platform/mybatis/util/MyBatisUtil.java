package com.bkjk.platform.mybatis.util;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * @Program: summerframework-samples
 * @Description: 从 Entity:getName 获取数据库字段名称，用于拼接查询用的 Map 参数
 * @Author: shaoze.wang
 * @Create: 2019/2/20 17:39
 **/
public class MyBatisUtil {

    public static <T> String getFieldName(EntityGetterMethod<T, Object> expression) {
        if (expression == null)
            throw new IllegalArgumentException("Expression should not be null");
        try {
            Method method = expression.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda)method.invoke(expression);
            String fieldName = StringUtils.resolveFieldName(serializedLambda.getImplMethodName());
            String className = serializedLambda.getImplClass().replace("/", ".");
            Field field = ReflectionUtils.findField(Class.forName(className), fieldName);
            String columnName = field.getName();
            TableField[] tableField = field.getAnnotationsByType(TableField.class);
            if (null != tableField && tableField.length == 1) {
                if (!StringUtils.isEmpty(tableField[0].value())) {
                    columnName = tableField[0].value();
                }
            }
            String ret = StringUtils.camelToUnderline(columnName);
            return ret;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("This will never happen!", e);
        }
    }

    public interface EntityGetterMethod<T, R> extends Function<T, R>, Serializable {
    }

}
