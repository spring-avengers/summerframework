package com.bkjk.platfrom.dts.core.resource.mysql.rollback;

import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.*;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class InsertRollback extends AbstractRollback {

    private static InsertRollback instance = null;

    public static InsertRollback getInstance() {
        if (instance == null) {
            synchronized (InsertRollback.class) {
                if (instance == null) {
                    instance = new InsertRollback();
                }
            }
        }
        return instance;
    }

    private Logger logger = LoggerFactory.getLogger(InsertRollback.class);

    private PreparedStatement assembleQuerySql(TxcLine txcLine, String tableName, Connection connection)
        throws SQLException {
        List<TxcField> txcFields = txcLine.getFields();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("select ");
        for (int j = 0; j < txcFields.size(); j++) {
            if (j != txcFields.size() - 1) {
                stringBuffer.append(txcFields.get(j).getSqlName()).append(",");
            } else {
                stringBuffer.append(txcFields.get(j).getSqlName());
            }
        }
        stringBuffer.append(" from ").append(tableName).append(" where ").append(txcLine.getPkCondition());
        String sql = stringBuffer.toString();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        for (PkPair<String, Object> primaryKeyValue : txcLine.getPrimaryKeyValues()) {
            preparedStatement.setObject(txcLine.getPrimaryKeyValues().indexOf(primaryKeyValue) + 1,
                primaryKeyValue.getValue());
        }
        return preparedStatement;
    }

    @Override
    protected List<PreparedStatement> assembleRollbackSql(CommitInfo commitInfo, Connection connection)
        throws SQLException {
        TxcLine txcLine = commitInfo.getPresentValue().getLine().get(0);
        String tableName = commitInfo.getPresentValue().getTableName();
        String sql = "delete from " + tableName + " where " + txcLine.getPkCondition();
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        for (PkPair<String, Object> primaryKeyValue : txcLine.getPrimaryKeyValues()) {
            preparedStatement.setObject(txcLine.getPrimaryKeyValues().indexOf(primaryKeyValue) + 1,
                primaryKeyValue.getValue());
        }
        return Lists.newArrayList(preparedStatement);
    }

    @Override
    protected boolean canRollback(CommitInfo commitInfo, Connection connection) throws SQLException {
        List<TxcLine> txcLines = commitInfo.getPresentValue().getLine();
        if (txcLines.size() > 1) {
            return false;
        }
        if (txcLines.size() == 0) {
            return false;
        }
        TxcLine txcLine = txcLines.get(0);
        PreparedStatement preparedStatement =
            assembleQuerySql(txcLine, commitInfo.getPresentValue().getTableName(), connection);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<TxcLine> dbValue = ResultConvertUtils.convertWithPrimary(resultSet, TableMetaUtils
            .getTableMetaInfo(connection, commitInfo.getPresentValue().getTableName()).getPrimaryKeyName(),
            SQLType.SELECT);
        dbValue.get(0).setPrimaryKeyValues(txcLines.get(0).getPrimaryKeyValues());
        boolean diff = DiffUtils.diff(txcLines.get(0), dbValue.get(0));
        if (!diff) {
            try {
                logger.error("data conflict, before:{},after:{}",
                    DiffUtils.getObjectMapper().writeValueAsString(txcLines),
                    DiffUtils.getObjectMapper().writeValueAsString(dbValue));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return false;
        }
        return true;
    }
}
