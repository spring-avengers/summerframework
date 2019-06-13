package com.bkjk.platfrom.dts.core.resource.mysql.parser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.bkjk.platfrom.dts.core.resource.mysql.StatementAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SerializeUtils;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaInfo;

public class InsertParser extends AbstractParser<MySqlInsertStatement> {

    private static InsertParser instance = null;

    public static InsertParser getInstance() {
        if (instance == null) {
            synchronized (InsertParser.class) {
                if (instance == null) {
                    instance = new InsertParser();
                }
            }
        }
        return instance;
    }

    private Object getAutoIncrementPrimaryKeyValue(Statement sourceStatement) throws SQLException {
        ResultSet rs = null;
        try {
            try {
                rs = sourceStatement.getGeneratedKeys();
            } catch (SQLException e) {
                if (e.getSQLState().equalsIgnoreCase("S1009")) {
                    rs = sourceStatement.executeQuery("SELECT LAST_INSERT_ID()");
                }
            }
            if (rs != null && rs.next()) {
                Object obj = rs.getObject(1);
                return obj;
            }
        } finally {
            if (rs != null)
                rs.close();
        }
        return null;
    }

    @Override
    public TableDataInfo getOriginValue(List<Object> whereParamsList, MySqlInsertStatement parseSqlStatement,
        StatementAdapter statementAdapter, TableMetaInfo tableMetaInfo) {
        return null;
    }

    @Override
    public TableDataInfo getPresentValue(List<Object> sqlParamsList, MySqlInsertStatement parseSqlStatement,
        StatementAdapter statementAdapter, TableMetaInfo tableMetaInfo) throws SQLException {
        TableDataInfo txcTable = new TableDataInfo();
        txcTable.setTableName(parseSqlStatement.getTableName().getSimpleName());
        List<TxcLine> line = txcTable.getLine();
        List<SQLInsertStatement.ValuesClause> valuesList = parseSqlStatement.getValuesList();
        List<SQLExpr> columns = parseSqlStatement.getColumns();
        for (SQLInsertStatement.ValuesClause valuesClause : valuesList) {
            List<SQLExpr> values = valuesClause.getValues();
            TxcLine txcLine = new TxcLine();
            for (int i = 0; i < columns.size(); i++) {
                TxcField txcField = new TxcField();
                String columnName = SQLUtils.toSQLString(columns.get(i)).replace("\'", "").replace("`", "").trim();
                txcField.setName(columnName);
                if (sqlParamsList != null && !sqlParamsList.isEmpty()) {
                    if (columnName.equalsIgnoreCase(tableMetaInfo.getAutoIncrementPrimaryKey())) {
                        sqlParamsList.add(i, getAutoIncrementPrimaryKeyValue(statementAdapter.getStatement()));
                    }
                    txcField.setValue(sqlParamsList.get(i));
                } else {
                    txcField.setValue(SQLUtils.toSQLString(values.get(i)));
                }
                txcField.setJdkValue(SerializeUtils.serialize(txcField.getValue()));
                txcLine.getFields().add(txcField);
            }
            line.add(txcLine);
        }
        return txcTable;
    }

    @Override
    public SQLType getSqlType() {
        return SQLType.INSERT;
    }

    @Override
    protected String getTableName(MySqlInsertStatement parseSqlStatement) {
        return parseSqlStatement.getTableName().getSimpleName();
    }

    @Override
    protected String selectSql(MySqlInsertStatement parseSqlStatement, Set<String> primaryKeyNameSet) {
        throw new UnsupportedOperationException(" do not support select sql for insert parser");
    }
}
