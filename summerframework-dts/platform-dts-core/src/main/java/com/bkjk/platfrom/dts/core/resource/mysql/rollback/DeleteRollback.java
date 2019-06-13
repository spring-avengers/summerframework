package com.bkjk.platfrom.dts.core.resource.mysql.rollback;

import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.PkPair;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SerializeUtils;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableDataInfo.TxcLine.TxcField;
import com.google.common.collect.Lists;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DeleteRollback extends AbstractRollback {

    private static DeleteRollback instance = null;

    public static DeleteRollback getInstance() {
        if (instance == null) {
            synchronized (DeleteRollback.class) {
                if (instance == null) {
                    instance = new DeleteRollback();
                }
            }
        }
        return instance;
    }

    @Override
    protected List<PreparedStatement> assembleRollbackSql(CommitInfo commitInfo, Connection connection)
        throws SQLException {

        ArrayList<PreparedStatement> preparedStatements = Lists.newArrayList();
        String tableName = commitInfo.getOriginalValue().getTableName();

        for (TxcLine txcLine : commitInfo.getOriginalValue().getLine()) {
            List<TxcField> txcFields = txcLine.getFields();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert into ").append(tableName).append("(");

            for (PkPair<String, Object> pkPair : txcLine.getPrimaryKeyValues()) {
                if ((txcLine.getPrimaryKeyValues().indexOf(pkPair) + 1) == txcLine.getPrimaryKeyValues().size()) {
                    if (txcFields.size() == 0) {
                        stringBuilder.append("`" + pkPair.getKey() + "`").append(")");
                    } else {
                        stringBuilder.append("`" + pkPair.getKey() + "`").append(",");
                    }
                } else {
                    stringBuilder.append("`" + pkPair.getKey() + "`").append(",");
                }
            }
            for (int i = 0; i < txcFields.size(); i++) {
                if (i == txcFields.size() - 1) {
                    stringBuilder.append(txcFields.get(i).getSqlName()).append(")");
                } else {
                    stringBuilder.append(txcFields.get(i).getSqlName()).append(",");
                }
            }
            stringBuilder.append(" value ").append("(");

            for (PkPair<String, Object> pkPair : txcLine.getPrimaryKeyValues()) {
                if ((txcLine.getPrimaryKeyValues().indexOf(pkPair) + 1) == txcLine.getPrimaryKeyValues().size()) {
                    if (txcFields.size() == 0) {
                        stringBuilder.append("?").append(")");
                    } else {
                        stringBuilder.append("?").append(",");
                    }
                } else {
                    stringBuilder.append("?").append(",");
                }
            }
            for (int i = 0; i < txcFields.size(); i++) {
                if (i == txcFields.size() - 1) {
                    stringBuilder.append("?").append(")");
                } else {
                    stringBuilder.append("?").append(",");
                }
            }
            String sql = stringBuilder.toString();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            for (PkPair<String, Object> pkPair : txcLine.getPrimaryKeyValues()) {
                preparedStatement.setObject(txcLine.getPrimaryKeyValues().indexOf(pkPair) + 1, pkPair.getValue());
            }
            for (int i = 1 + txcLine.getPrimaryKeyValues().size();
                i <= txcFields.size() + txcLine.getPrimaryKeyValues().size(); i++) {
                preparedStatement.setObject(i, SerializeUtils
                    .derialize(txcFields.get(i - 1 - txcLine.getPrimaryKeyValues().size()).getJdkValue()));
            }
            preparedStatements.add(preparedStatement);
        }

        return preparedStatements;
    }

    @Override
    protected boolean canRollback(CommitInfo commitInfo, Connection connection) throws SQLException {
        if (commitInfo.getOriginalValue().getLine().size() == 0) {
            return false;
        }
        return true;

    }
}
