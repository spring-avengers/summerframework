package com.bkjk.platfrom.dts.core.resource.mysql.rollback;

import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.*;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.bkjk.platfrom.dts.core.resource.mysql.parser.UpdateParser;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdateRollback extends AbstractRollback {
    private static UpdateRollback instance = null;

    public static UpdateRollback getInstance() {
        if (instance == null) {
            synchronized (UpdateRollback.class) {
                if (instance == null) {
                    instance = new UpdateRollback();
                }
            }
        }
        return instance;
    }

    private Logger logger = LoggerFactory.getLogger(UpdateRollback.class);

    @Override
    protected List<PreparedStatement> assembleRollbackSql(CommitInfo commitInfo, Connection connection)
        throws SQLException {
        ArrayList<PreparedStatement> preparedStatements = Lists.newArrayList();
        String tableName = commitInfo.getOriginalValue().getTableName();
        for (TxcLine txcLine : commitInfo.getOriginalValue().getLine()) {
            List<TxcField> txcFields = txcLine.getFields();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update ").append(tableName).append(" ").append("set ");
            for (int i = 0; i < txcFields.size(); i++) {
                if (i == txcFields.size() - 1) {
                    stringBuilder.append(txcFields.get(i).getSqlName()).append("=").append("?");
                } else {
                    stringBuilder.append(txcFields.get(i).getSqlName()).append("=").append("?").append(",");
                }
            }
            String sql = stringBuilder.append(" where ").append(txcLine.getPkCondition()).toString();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            for (int j = 1; j <= txcFields.size(); j++) {
                preparedStatement.setObject(j, SerializeUtils.derialize(txcFields.get(j - 1).getJdkValue()));
            }
            for (PkPair<String, Object> primaryKeyValue : txcLine.getPrimaryKeyValues()) {
                preparedStatement.setObject(
                    txcLine.getPrimaryKeyValues().indexOf(primaryKeyValue) + 1 + txcFields.size(),
                    primaryKeyValue.getValue());
            }
            preparedStatements.add(preparedStatement);
        }
        return preparedStatements;

    }

    @Override
    protected boolean canRollback(CommitInfo commitInfo, Connection connection) throws SQLException {
        String sql = commitInfo.getSql();
        SQLUpdateStatement sqlParseStatement = (SQLUpdateStatement)new MySqlStatementParser(sql).parseStatement();
        TableMetaInfo tableMetaInfo =
            TableMetaUtils.getTableMetaInfo(connection, sqlParseStatement.getTableName().getSimpleName());
        TableDataInfo dbValue = UpdateParser.getInstance().getOriginValue(commitInfo.getWhereParams(),
            sqlParseStatement, connection, tableMetaInfo);
        if (commitInfo.getOriginalValue().getLine().size() == 0) {
            return false;
        }
        for (TxcLine txcLine : dbValue.getLine()) {
            txcLine.setPrimaryKeyValues(commitInfo.getPresentValue().getLine().get(0).getPrimaryKeyValues());
            boolean diff = DiffUtils.diff(commitInfo.getPresentValue().getLine().get(0), txcLine);
            if (!diff) {
                try {
                    logger.error("data conflict, before:{},after:{}",
                        DiffUtils.getObjectMapper().writeValueAsString(commitInfo.getPresentValue().getLine().get(0)),
                        DiffUtils.getObjectMapper().writeValueAsString(txcLine));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                return false;
            }
        }
        return true;
    }

}
