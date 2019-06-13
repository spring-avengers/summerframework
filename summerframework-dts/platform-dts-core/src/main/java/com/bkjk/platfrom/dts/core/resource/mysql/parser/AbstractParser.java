package com.bkjk.platfrom.dts.core.resource.mysql.parser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.PreparedStatementAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.StatementAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.common.ResultConvertUtils;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaUtils;
import org.springframework.util.CollectionUtils;

@SuppressWarnings("unchecked")
public abstract class AbstractParser<T> {

    public TableDataInfo getOriginValue(List<Object> whereParamsList, T parseSqlStatement, Connection connection,
        TableMetaInfo tableMetaInfo) throws SQLException {
        TableDataInfo txcTable = new TableDataInfo();
        txcTable.setTableName(getTableName(parseSqlStatement));
        Set<String> primaryKeyName = tableMetaInfo.getPrimaryKeyName();
        String selectSql = selectSql(parseSqlStatement, primaryKeyName);
        PreparedStatement preparedStatement = connection.prepareStatement(selectSql);
        if (whereParamsList != null && !whereParamsList.isEmpty()) {
            for (int i = 1; i <= whereParamsList.size(); i++) {
                preparedStatement.setObject(i, whereParamsList.get(i - 1));
            }
        }
        ResultSet resultSet = preparedStatement.executeQuery();
        List<TxcLine> txcLines = ResultConvertUtils.convertWithPrimary(resultSet, primaryKeyName, getSqlType());
        txcTable.setLine(txcLines);
        return txcTable;
    }

    public TableDataInfo getOriginValue(List<Object> whereParamsList, T parseSqlStatement,
        StatementAdapter statementAdapter, TableMetaInfo tableMetaInfo) throws SQLException {
        return this.getOriginValue(whereParamsList, parseSqlStatement, statementAdapter.getConnection(), tableMetaInfo);
    }

    protected TableDataInfo getPresentValue(List<Object> sqlParamsList, T parseSqlStatement,
        StatementAdapter statementAdapter, TableMetaInfo tableMetaInfo) throws SQLException {
        return null;
    }

    protected SQLType getSqlType() {
        return null;
    }

    protected String getTableName(T parseSqlStatement) {
        return null;
    }

    protected String getWhere(T parseSqlStatement) {
        return null;
    }

    protected List<Object> getWhereParams(List<Object> sqlParamsList, T parseSqlStatement) {
        return null;
    }

    protected CommitInfo parse(StatementAdapter statementAdapter) throws SQLException {
        CommitInfo commitInfo = new CommitInfo();
        String sql = statementAdapter.getSql();
        T sqlParseStatement = (T)new MySqlStatementParser(sql).parseStatement();

        commitInfo.setSqlType(getSqlType());

        commitInfo.setWhere(getWhere(sqlParseStatement));

        commitInfo.setSql(sql);
        if (statementAdapter instanceof PreparedStatementAdapter) {
            PreparedStatementAdapter preparedStatementAdapter = (PreparedStatementAdapter)statementAdapter;
            commitInfo.setSqlParams(preparedStatementAdapter.getParamsList());
            commitInfo.setWhereParams(getWhereParams(preparedStatementAdapter.getParamsList(), sqlParseStatement));
        }

        TableMetaInfo tableMetaInfo =
            TableMetaUtils.getTableMetaInfo(statementAdapter.getConnection(), getTableName(sqlParseStatement));
        if (CollectionUtils.isEmpty(tableMetaInfo.getPrimaryKeyName())) {
            return null;
        }
        TableDataInfo originValue =
            getOriginValue(commitInfo.getWhereParams(), sqlParseStatement, statementAdapter, tableMetaInfo);
        commitInfo.setOriginalValue(originValue);

        TableDataInfo presentValue =
            getPresentValue(commitInfo.getSqlParams(), sqlParseStatement, statementAdapter, tableMetaInfo);
        commitInfo.setPresentValue(presentValue);
        return commitInfo;

    }

    protected String selectSql(T parseSqlStatement, Set<String> primaryKeyName) {
        return null;
    }

}
