package com.bkjk.platform.mybatis.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Program: summerframework2
 * @Description:
 * @Author: shaoze.wang
 * @Create: 2019/4/9 15:15
 **/
public class SmartEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {
    private Class<E> type;

    private EnumOrdinalTypeHandler fallback;

    public SmartEnumTypeHandler() {
    }

    public SmartEnumTypeHandler(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
        this.fallback=new EnumOrdinalTypeHandler(type);
    }

    public final static String GET_VALUE_METHOD_NAME ="getValue";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        Method method = ReflectionUtils.findMethod(parameter.getClass(), GET_VALUE_METHOD_NAME);
        try {
            ps.setObject(i,method.invoke(parameter));
        } catch (Exception e) {
            fallback.setNonNullParameter(ps,i,parameter,jdbcType);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        try {
            return (E)getEnumOrNull(type,rs.getObject(columnName));
        } catch (Exception e) {
            return (E) fallback.getNullableResult(rs,columnName);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        try {
            return (E)getEnumOrNull(type,value);
        } catch (Exception e) {
            return (E) fallback.getNullableResult(rs,columnIndex);
        }
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        try {
            return (E) getEnumOrNull(type,value);
        } catch (Exception e) {
            return (E) fallback.getNullableResult(cs,columnIndex);
        }
    }

    private static final <E> E getEnumOrNull(Class<E> type,Object value){
        if(null==value){
            return null;
        }
        Method method = null;
        try {
            method = type.getDeclaredMethod("values");
            Object[] values= (Object[]) method.invoke(type);
            Method valuesMethod = ReflectionUtils.findMethod(type, GET_VALUE_METHOD_NAME);
            for (int i = 0; i < values.length; i++) {
                Object code=valuesMethod.invoke(values[i]);
                if(code.getClass().equals(value.getClass())){
                    //仅当类型一致时，用equals做比较
                    if(code!=null&&code.equals(value)){
                        return (E) values[i];
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }

}
