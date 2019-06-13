package com.bkjk.platfrom.dts.core.resource.mysql.parser;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.bkjk.platfrom.dts.core.resource.mysql.StatementAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaInfo;
import com.google.common.collect.Lists;

public class DeleteParser extends AbstractParser<MySqlDeleteStatement> {
    private static DeleteParser instance = null;

    public static DeleteParser getInstance() {
        if (instance == null) {
            synchronized (DeleteParser.class) {
                if (instance == null) {
                    instance = new DeleteParser();
                }
            }
        }
        return instance;
    }

    @Override
    public TableDataInfo getPresentValue(List<Object> sqlParamsList, MySqlDeleteStatement parseSqlStatement,
        StatementAdapter statementAdapter, TableMetaInfo tableMetaInfo) throws SQLException {
        return null;
    }

    @Override
    public SQLType getSqlType() {
        return SQLType.DELETE;
    }

    @Override
    protected String getTableName(MySqlDeleteStatement parseSqlStatement) {
        return parseSqlStatement.getTableName().getSimpleName();
    }

    @Override
    protected String getWhere(MySqlDeleteStatement parseSqlStatement) {
        return SQLUtils.toSQLString(parseSqlStatement.getWhere());
    }

    @Override
    protected List<Object> getWhereParams(List<Object> sqlParamsList, MySqlDeleteStatement parseSqlStatement) {
        if (sqlParamsList != null && !sqlParamsList.isEmpty()) {
            return sqlParamsList;
        }
        return Lists.newArrayList();
    }

    @Override
    protected String selectSql(MySqlDeleteStatement mySqlUpdateStatement, Set<String> primaryKeyNameSet) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("SELECT * ");
        stringBuffer.append(" from ").append(mySqlUpdateStatement.getTableName().getSimpleName()).append(" where ");
        stringBuffer.append(SQLUtils.toSQLString(mySqlUpdateStatement.getWhere()));
        return stringBuffer.toString();
    }

}
