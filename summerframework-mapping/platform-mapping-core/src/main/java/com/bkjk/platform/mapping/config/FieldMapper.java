package com.bkjk.platform.mapping.config;

import com.bkjk.platform.mapping.util.Expression;
import com.bkjk.platform.mapping.util.ExpressionUtil;

public class FieldMapper<A, B> {
    private String fieldOfA;
    private String fieldOfB;
    private boolean skip = false;
    private String converter;
    private FieldMapperDirection direction = FieldMapperDirection.BI_DIRECTION;

    public FieldMapper(Expression<A, Object> field, boolean skip) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(field);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(field);
        this.skip = skip;
    }

    public FieldMapper(Expression<A, Object> field, boolean skip, FieldMapperDirection direction) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(field);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(field);
        this.skip = skip;
        this.direction = direction;
    }

    public FieldMapper(Expression<A, Object> fieldOfA, Expression<B, Object> fieldOfB) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(fieldOfA);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(fieldOfB);
    }

    public FieldMapper(Expression<A, Object> fieldOfA, Expression<B, Object> fieldOfB, boolean skip) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(fieldOfA);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(fieldOfB);
        this.skip = skip;
    }

    public FieldMapper(Expression<A, Object> fieldOfA, Expression<B, Object> fieldOfB, boolean skip,
        FieldMapperDirection direction) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(fieldOfA);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(fieldOfB);
        this.skip = skip;
        this.direction = direction;
    }

    public FieldMapper(Expression<A, Object> fieldOfA, Expression<B, Object> fieldOfB, String converter) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(fieldOfA);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(fieldOfB);
        this.converter = converter;
    }

    public FieldMapper(Expression<A, Object> fieldOfA, Expression<B, Object> fieldOfB, String converter,
        FieldMapperDirection direction) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(fieldOfA);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(fieldOfB);
        this.converter = converter;
        this.direction = direction;
    }

    public FieldMapper(Expression<A, Object> field, String converter) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(field);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(field);
        this.converter = converter;
    }

    public FieldMapper(Expression<A, Object> field, String converter, FieldMapperDirection direction) {
        this.fieldOfA = ExpressionUtil.expressionToFieldName(field);
        this.fieldOfB = ExpressionUtil.expressionToFieldName(field);
        this.converter = converter;
        this.direction = direction;
    }

    public String getConverter() {
        return converter;
    }

    public FieldMapperDirection getDirection() {
        return direction;
    }

    public String getFieldOfA() {
        return fieldOfA;
    }

    public String getFieldOfB() {
        return fieldOfB;
    }

    public boolean isSkip() {
        return skip;
    }

}
