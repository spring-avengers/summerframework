package com.bkjk.platfrom.dts.core.resource.mysql.parser;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.bkjk.platfrom.dts.core.resource.mysql.StatementAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SerializeUtils;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaInfo;
import com.google.common.collect.Lists;

public class UpdateParser extends AbstractParser<SQLUpdateStatement> {

    private static UpdateParser instance = null;

    public static UpdateParser getInstance() {
        if (instance == null) {
            synchronized (UpdateParser.class) {
                if (instance == null) {
                    instance = new UpdateParser();
                }
            }
        }
        return instance;
    }

    @Override
    public TableDataInfo getPresentValue(List<Object> sqlParamsList, SQLUpdateStatement parseSqlStatement,
        StatementAdapter statementAdapter, TableMetaInfo tableMetaInfo) throws SQLException {
        TableDataInfo txcTable = new TableDataInfo();
        txcTable.setTableName(parseSqlStatement.getTableName().getSimpleName());
        TxcLine txcLine = new TxcLine();
        List<SQLUpdateSetItem> items = parseSqlStatement.getItems();
        int variantExpr = 0;
        for (int i = 0; i < items.size(); i++) {
            SQLUpdateSetItem sqlUpdateSetItem = items.get(i);
            TxcField txcField = new TxcField();
            String cloumnName =
                SQLUtils.toSQLString(sqlUpdateSetItem.getColumn()).replace("\'", "").replace("`", "").trim();
            txcField.setName(cloumnName);
            if (sqlUpdateSetItem.getValue() instanceof SQLVariantRefExpr) {
                txcField.setValue(sqlParamsList.get(variantExpr++));
            } else if (sqlUpdateSetItem.getValue() instanceof SQLValuableExpr) {
                txcField.setValue(SQLUtils.toSQLString(items.get(i).getValue()));
            } else {
                throw new UnsupportedOperationException(
                    String.format("Do not support complex sql,%s", sqlUpdateSetItem.getClass().toString()));
            }
            txcField.setJdkValue(SerializeUtils.serialize(txcField.getValue()));
            txcLine.getFields().add(txcField);
        }
        txcTable.getLine().add(txcLine);
        return txcTable;
    }

    @Override
    public SQLType getSqlType() {
        return SQLType.UPDATE;
    }

    @Override
    protected String getTableName(SQLUpdateStatement parseSqlStatement) {
        return parseSqlStatement.getTableName().getSimpleName();
    }

    @Override
    protected String getWhere(SQLUpdateStatement parseSqlStatement) {
        return SQLUtils.toSQLString(parseSqlStatement.getWhere());
    }

    @Override
    protected List<Object> getWhereParams(List<Object> sqlParamsList, SQLUpdateStatement parseSqlStatement) {
        if (sqlParamsList != null && !sqlParamsList.isEmpty()) {
            int size = 0;
            for (SQLUpdateSetItem sqlUpdateSetItem : parseSqlStatement.getItems()) {
                if (sqlUpdateSetItem.getValue() instanceof SQLVariantRefExpr) {
                    size++;
                }
            }
            return Lists.newArrayList(sqlParamsList.subList(size, sqlParamsList.size()));
        }
        return Lists.newArrayList();
    }

    @Override
    protected String selectSql(SQLUpdateStatement mySqlUpdateStatement, Set<String> primaryKeyNameSet) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("SELECT ");
        List<SQLUpdateSetItem> items = mySqlUpdateStatement.getItems();
        for (SQLUpdateSetItem sqlUpdateSetItem : items) {
            stringBuffer.append(SQLUtils.toSQLString(sqlUpdateSetItem.getColumn())).append(",");
        }
        stringBuffer.append(String.join(",", primaryKeyNameSet));
        stringBuffer.append(" from ").append(mySqlUpdateStatement.getTableName().getSimpleName()).append(" where ");
        stringBuffer.append(SQLUtils.toSQLString(mySqlUpdateStatement.getWhere()));
        return stringBuffer.toString();
    }

}
