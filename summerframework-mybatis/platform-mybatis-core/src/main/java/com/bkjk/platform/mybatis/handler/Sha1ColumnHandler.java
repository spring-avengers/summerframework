package com.bkjk.platform.mybatis.handler;


import com.bkjk.platform.mybatis.encrypt.Sha1Column;
import com.bkjk.platform.mybatis.encrypt.Sha1HexUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.bkjk.platform.mybatis.PlatformMybatisApplicationRunListener.SHA1_COLUMN_HANDLER_SALT;

public class Sha1ColumnHandler extends BaseTypeHandler<Sha1Column> {

    public static final String PREFIX = "shadow:";

    public static final String hash(String value) {
        String salt = System.getProperty(SHA1_COLUMN_HANDLER_SALT, "");
        return Sha1HexUtil.sha1Hex(salt + value);
    }

    private Sha1Column get(String value) {
        if (value == null) {
            return null;
        }
        return Sha1Column.create(value, false);
    }

    @Override
    public Sha1Column getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String ret = cs.getString(columnIndex);
        return get(ret);
    }

    @Override
    public Sha1Column getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String ret = rs.getString(columnIndex);
        return get(ret);
    }

    @Override
    public Sha1Column getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String ret = rs.getString(columnName);
        return get(ret);
    }

    private boolean needShadow(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Sha1Column parameter, JdbcType jdbcType)
        throws SQLException {
        ps.setString(i, needShadow(parameter.getValue()) ? hash(parameter.getValue().substring(PREFIX.length()))
            : parameter.getValue());
    }
}
