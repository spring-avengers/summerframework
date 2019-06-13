package com.bkjk.platform.mapping.util;

import java.beans.Introspector;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

public class ExpressionUtil {
    public static String expressionToFieldName(Expression expression) {
        return Introspector.decapitalize(expressionToMethodName(expression).replace("get", ""));
    }

    public static String expressionToMethodName(Expression expression) {
        if (expression == null)
            return "";
        try {
            Method method = expression.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(Boolean.TRUE);
            SerializedLambda serializedLambda = (SerializedLambda)method.invoke(expression);
            return serializedLambda.getImplMethodName();
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }
}
