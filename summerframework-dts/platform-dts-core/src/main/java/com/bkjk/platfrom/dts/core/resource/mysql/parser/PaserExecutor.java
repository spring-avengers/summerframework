package com.bkjk.platfrom.dts.core.resource.mysql.parser;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext;
import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.ResourceRowLockHelper;
import com.bkjk.platfrom.dts.core.resource.mysql.StatementAdapter;
import com.bkjk.platfrom.dts.core.resource.mysql.common.PkPair;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PaserExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PaserExecutor.class);

    public static void after(StatementAdapter txcStatement, SQLType sqlType) {
        try {
            if (sqlType == SQLType.INSERT) {
                DbRuntimeContext txcRuntimeContext = txcStatement.getConnection().getConnectionRuntimeContext();

                doInsertParse(txcStatement);
                List<CommitInfo> commitInfos = txcRuntimeContext.getInfo();
                if (commitInfos.size() == 0) {
                    return;
                }
                CommitInfo commitInfo = commitInfos.get(commitInfos.size() - 1);
                List<TxcLine> line = commitInfo.getPresentValue().getLine();
                if (line.size() > 1) {
                    logger.error("Do not support many insert sql");
                    return;
                }
                TxcLine txcLine = line.get(0);
                setPrimaryValue(txcStatement, commitInfo, txcLine);

                fillDbMetaAndLockRow(txcStatement, commitInfo);
            }
        } catch (SQLException e) {
            logger.error("execute parser after error", e);
        }
    }

    private static void doInsertParse(StatementAdapter txcStatement) {
        try {
            CommitInfo commitInfo = InsertParser.getInstance().parse(txcStatement);
            if (!Objects.isNull(commitInfo)) {
                DbRuntimeContext txcRuntimeContext = txcStatement.getConnection().getConnectionRuntimeContext();
                txcRuntimeContext.getInfo().add(commitInfo);
            }
        } catch (Exception e) {
            logger.error("parse sql error", e);
        }
    }

    private static void fillDbMetaAndLockRow(StatementAdapter txcStatement, CommitInfo commitInfo) throws SQLException {
        if (commitInfo != null && commitInfo.getSchemaName() == null) {
            String dbName = TableMetaUtils.getDbNameFromUrl(txcStatement.getConnection().getMetaData().getURL());
            commitInfo.setSchemaName(dbName);
            String uniqueDbId = txcStatement.getConnection().getDataSource().getUniqueDbId();
            commitInfo.setUniqueDbId(uniqueDbId);
            String tableName = SQLType.INSERT == commitInfo.getSqlType() ? commitInfo.getPresentValue().getTableName()
                : commitInfo.getOriginalValue().getTableName();
            List<TxcLine> txcLinesWithPrimaryInfo = SQLType.INSERT == commitInfo.getSqlType()
                ? commitInfo.getPresentValue().getLine() : commitInfo.getOriginalValue().getLine();
            for (TxcLine txcLine : txcLinesWithPrimaryInfo) {
                String row_key = ResourceRowLockHelper.buildRowKey(txcLine.getPrimaryKeyValues());
                // 添加行锁
                try {
                    ResourceRowLockHelper.insertRowLock(txcStatement.getConnection(),
                        txcStatement.getConnection().getConnectionRuntimeContext(), tableName, row_key);
                } catch (SQLException e) {
                    logger.error("add row lock failed,will occur dirty reading in app", e);
                    throw e;
                }
            }
        }
    }

    public static SQLType parse(StatementAdapter txcStatement) throws SQLException {
        long start = System.currentTimeMillis();
        SQLType sqlType = SQLType.SELECT;
        try {
            DbRuntimeContext txcRuntimeContext = txcStatement.getConnection().getConnectionRuntimeContext();
            String sql = txcStatement.getSql();
            SQLStatement sqlParseStatement = new MySqlStatementParser(sql).parseStatement();
            CommitInfo commitInfo = null;
            if (sqlParseStatement instanceof MySqlUpdateStatement) {
                commitInfo = UpdateParser.getInstance().parse(txcStatement);
                sqlType = SQLType.UPDATE;
                if (!Objects.isNull(commitInfo)&&!CollectionUtils.isEmpty(commitInfo.getOriginalValue().getLine())) {
                    txcRuntimeContext.getInfo().add(commitInfo);
                    fillDbMetaAndLockRow(txcStatement, commitInfo);
                }
            } else if (sqlParseStatement instanceof MySqlInsertStatement) {
                sqlType = SQLType.INSERT;
            } else if (sqlParseStatement instanceof MySqlDeleteStatement) {
                commitInfo = DeleteParser.getInstance().parse(txcStatement);
                sqlType = SQLType.DELETE;
                if (!Objects.isNull(commitInfo) && !CollectionUtils.isEmpty(commitInfo.getOriginalValue().getLine())) {
                    txcRuntimeContext.getInfo().add(commitInfo);
                    fillDbMetaAndLockRow(txcStatement, commitInfo);
                }
            } else if (sqlParseStatement instanceof SQLSelectStatement) {
                SQLSelectQueryBlock selectQueryBlock =
                    ((SQLSelectStatement)sqlParseStatement).getSelect().getQueryBlock();
                if (selectQueryBlock.getFrom() != null) {
                    SelectParser.getInstance().parse(txcStatement);
                    sqlType = SQLType.SELECT;
                }
            }
        } catch (Exception e) {
            logger.error("parse sql error", e);
            if (e instanceof SQLException || e instanceof RuntimeException) {
                throw e;
            } else {
                throw new SQLException(e);
            }
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (sqlType != SQLType.SELECT || cost > 50) {
                logger.debug("parser sql:{}, cost:{}ms", txcStatement.getSql(), cost);
            }
        }
        return sqlType;
    }

    private static void setPrimaryValue(StatementAdapter txcStatement, CommitInfo commitInfo, TxcLine txcLine)
        throws SQLException {
        TableMetaInfo tableMetaInfo =
            TableMetaUtils.getTableMetaInfo(txcStatement.getConnection(), commitInfo.getPresentValue().getTableName());
        String autoIncrementPrimaryKey = tableMetaInfo.getAutoIncrementPrimaryKey();
        if (StringUtils.isBlank(autoIncrementPrimaryKey)) {
            Set<String> primaryKeyNameSet = tableMetaInfo.getPrimaryKeyName();
            for (String primaryKeyName : primaryKeyNameSet) {
                for (TxcField txcField : txcLine.getFields()) {
                    if (txcField.getName().equals(primaryKeyName)) {
                        txcLine.getPrimaryKeyValues().add(PkPair.of(primaryKeyName, txcField.getValue()));
                        break;
                    }
                }
            }
        } else {
            ResultSet resultSet =
                txcStatement.getConnection().prepareStatement("select last_insert_id() as id").executeQuery();
            while (resultSet.next()) {
                txcLine.getPrimaryKeyValues().add(PkPair.of(autoIncrementPrimaryKey, resultSet.getObject("id")));
            }
        }
        List<TxcField> fields = Lists.newArrayList();
        for (TxcField txcField : txcLine.getFields()) {
            if (!tableMetaInfo.getPrimaryKeyName().contains(txcField.getName())) {
                fields.add(txcField);
            }
        }
        txcLine.getFields().clear();
        txcLine.getFields().addAll(fields);
    }
}
