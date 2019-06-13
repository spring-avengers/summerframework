package com.bkjk.platfrom.dts.core.resource.mysql.parser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.bkjk.platfrom.dts.core.resource.mysql.ConnectionAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.ResourceRowLockHelper;
import com.bkjk.platfrom.dts.core.resource.mysql.common.ResultConvertUtils;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaInfo;
import com.google.common.collect.Lists;

public class SelectParser extends AbstractParser<SQLSelectStatement> {

    private static SelectParser instance = null;

    public static SelectParser getInstance() {
        if (instance == null) {
            synchronized (SelectParser.class) {
                if (instance == null) {
                    instance = new SelectParser();
                }
            }
        }
        return instance;
    }

    Logger logger = LoggerFactory.getLogger(SelectParser.class);

    private int lockRetryInternal = 1000;

    private int lockRetryTimes = 3;

    @Override
    public TableDataInfo getOriginValue(List<Object> whereParamsList, SQLSelectStatement parseSqlStatement,
        Connection connection, TableMetaInfo tableMetaInfo) throws SQLException {
        Savepoint sp = null;
        TableDataInfo txcTable = new TableDataInfo();
        txcTable.setTableName(getTableName(parseSqlStatement));
        Set<String> primaryKeyNameSet = tableMetaInfo.getPrimaryKeyName();
        String selectSql = selectSql(parseSqlStatement, primaryKeyNameSet);
        LockRetryExecutor lockRetryExecutor = new LockRetryExecutor();
        Connection conn = ((ConnectionAdapter)connection).getSourceConnection();
        boolean originalAutoCommit = conn.getAutoCommit();
        try {
            if (originalAutoCommit) {
                conn.setAutoCommit(false);
            }
            sp = conn.setSavepoint();
            while (true) {

                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    preparedStatement = conn.prepareStatement(selectSql);
                    if (whereParamsList != null && !whereParamsList.isEmpty()) {
                        for (int i = 1; i <= whereParamsList.size(); i++) {
                            preparedStatement.setObject(i, whereParamsList.get(i - 1));
                        }
                    }
                    resultSet = preparedStatement.executeQuery();
                    List<TableDataInfo.TxcLine> txcLines =
                        ResultConvertUtils.convertWithPrimary(resultSet, primaryKeyNameSet, getSqlType());
                    txcTable.setLine(txcLines);
                    boolean allLocked = true;
                    for (TableDataInfo.TxcLine txcLine : txcLines) {
                        String row_key = ResourceRowLockHelper.buildRowKey(txcLine.getPrimaryKeyValues());
                        boolean locked = ResourceRowLockHelper.rowLocked(((ConnectionAdapter)connection),
                            ((ConnectionAdapter)connection).getConnectionRuntimeContext(), txcTable.getTableName(),
                            row_key);
                        if (locked) {
                            conn.rollback(sp);
                            lockRetryExecutor.sleep();
                            allLocked = false;
                            break;
                        }
                    }
                    if (allLocked) {
                        break;
                    }
                } catch (Throwable e) {
                    logger.error("Global lock for select failed", e);
                    conn.rollback(sp);
                    throw e;
                } finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                }
            }

        } finally {
            if (sp != null) {
                conn.releaseSavepoint(sp);
            }
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return null;
    }

    @Override
    public SQLType getSqlType() {
        return SQLType.SELECT;
    }

    @Override
    protected String getTableName(SQLSelectStatement parseSqlStatement) {
        SQLSelectQueryBlock selectQueryBlock = parseSqlStatement.getSelect().getQueryBlock();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        StringBuffer sb = new StringBuffer();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(sb) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit((SQLExprTableSource)tableSource);
        return sb.toString();
    }

    @Override
    protected String getWhere(SQLSelectStatement parseSqlStatement) {
        SQLSelectQueryBlock selectQueryBlock = parseSqlStatement.getSelect().getQueryBlock();
        SQLExpr where = selectQueryBlock.getWhere();
        if (where == null) {
            return "";
        }
        return SQLUtils.toSQLString(where);

    }

    @Override
    protected List<Object> getWhereParams(List<Object> sqlParamsList, SQLSelectStatement parseSqlStatement) {
        if (sqlParamsList != null && !sqlParamsList.isEmpty()) {
            return sqlParamsList;
        }
        return Lists.newArrayList();
    }

    @Override
    protected String selectSql(SQLSelectStatement mySqlSelectStatement, Set<String> primaryKeyNameSet) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("SELECT ");
        stringBuffer.append(String.join(",", primaryKeyNameSet));
        stringBuffer.append(" from ").append(getTableName(mySqlSelectStatement)).append(" where ");
        stringBuffer.append(getWhere(mySqlSelectStatement));
        return stringBuffer.toString();
    }

    public void sleep() {
        if (--lockRetryTimes < 0) {
            throw new RuntimeException("Global lock wait timeout");
        }

        try {
            Thread.sleep(lockRetryInternal);
        } catch (InterruptedException ignore) {
        }
    }

}
