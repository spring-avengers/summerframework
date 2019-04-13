package com.bkjk.platform.mybatis.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platform.mybatis.encrypt.EncryptedColumn;
import com.bkjk.platform.mybatis.encrypt.StringEncryptorHolder;

public class EncryptedColumnHandler extends BaseTypeHandler<EncryptedColumn> {
    public static final Logger logger = LoggerFactory.getLogger(EncryptedColumnHandler.class);

    private EncryptedColumn get(String value) {
        if (null == value) {
            return null;
        }
        try {
            return EncryptedColumn.create(StringEncryptorHolder.getStringEncryptor().decrypt(value));
        } catch (Throwable ex) {
            logger.error("Can not decrypt value {}", value);
            return EncryptedColumn.create(value);
        }
    }

    @Override
    public EncryptedColumn getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String ret = cs.getString(columnIndex);
        return get(ret);
    }

    @Override
    public EncryptedColumn getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String ret = rs.getString(columnIndex);
        return get(ret);
    }

    @Override
    public EncryptedColumn getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String ret = rs.getString(columnName);
        return get(ret);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, EncryptedColumn parameter, JdbcType jdbcType)
        throws SQLException {
        ps.setString(i, StringEncryptorHolder.getStringEncryptor().encrypt(parameter.getValue()));
    }
}
