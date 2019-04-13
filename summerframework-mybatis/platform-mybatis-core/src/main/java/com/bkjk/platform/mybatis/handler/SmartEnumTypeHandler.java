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

    public static String getValueMethodName ="getValue";
    public static String fromValueMethodName ="fromValue";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        Method method = ReflectionUtils.findMethod(parameter.getClass(), getValueMethodName);
        try {
            ps.setObject(i,method.invoke(parameter));
        } catch (Exception e) {
            fallback.setNonNullParameter(ps,i,parameter,jdbcType);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        Object value = rs.getObject(columnName);
        if(null==value){
            return null;
        }
        Method method = ReflectionUtils.findMethod(type, fromValueMethodName,value.getClass());
        try {
            return (E) method.invoke(type,rs.getObject(columnName));
        } catch (Exception e) {
            return (E) fallback.getNullableResult(rs,columnName);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        if(rs.wasNull()){
            return null;
        }
        Object value = rs.getObject(columnIndex);
        if(null==value){
            return null;
        }
        Method method = ReflectionUtils.findMethod(type, fromValueMethodName,value.getClass());
        try {
            return (E) method.invoke(type,value);
        } catch (Exception e) {
            return (E) fallback.getNullableResult(rs,columnIndex);
        }
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        if(cs.wasNull()){
            return null;
        }
        Object value = cs.getObject(columnIndex);
        if(null==value){
            return null;
        }
        Method method = ReflectionUtils.findMethod(type, fromValueMethodName,value.getClass());
        try {
            return (E) method.invoke(type,value);
        } catch (Exception e) {
            return (E) fallback.getNullableResult(cs,columnIndex);
        }
    }
}
