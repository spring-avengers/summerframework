package com.bkjk.platfrom.dts.core.resource.mysql.rollback;

import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.DiffUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public abstract class AbstractRollback {
    private Logger logger = LoggerFactory.getLogger(AbstractRollback.class);

    protected abstract List<PreparedStatement> assembleRollbackSql(CommitInfo commitInfo, Connection connection)
        throws SQLException;

    protected abstract boolean canRollback(CommitInfo commitInfo, Connection connection) throws SQLException;

    public boolean rollback(CommitInfo commitInfo, Connection connection) throws SQLException {

        boolean flag = canRollback(commitInfo, connection);

        if (flag) {
            logger.info("rollback for sql:{}", commitInfo.getSql());
            List<PreparedStatement> preparedStatements = assembleRollbackSql(commitInfo, connection);
            for (PreparedStatement preparedStatement : preparedStatements) {
                preparedStatement.execute();
                preparedStatement.close();
            }
            logger.info("rollback sql success");
            return true;
        }
        try {
            logger.error("{} cannot rollback data", DiffUtils.getObjectMapper().writeValueAsString(commitInfo));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }
}
