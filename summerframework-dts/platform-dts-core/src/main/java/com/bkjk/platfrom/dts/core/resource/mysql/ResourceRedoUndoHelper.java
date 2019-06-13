package com.bkjk.platfrom.dts.core.resource.mysql;

import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platfrom.dts.core.resource.mysql.DbRuntimeContext.CommitInfo;
import com.bkjk.platfrom.dts.core.resource.mysql.common.BlobUtils;
import com.bkjk.platfrom.dts.core.resource.mysql.common.SQLType;
import com.bkjk.platfrom.dts.core.resource.mysql.rollback.DeleteRollback;
import com.bkjk.platfrom.dts.core.resource.mysql.rollback.InsertRollback;
import com.bkjk.platfrom.dts.core.resource.mysql.rollback.UpdateRollback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class ResourceRedoUndoHelper {

    private static final Map<String, DataSource> DBNAME_DATASOURCE_CACHE = Maps.newConcurrentMap();

    private static final String DTS_BRANCH_INFO_TABLE = "dts_branch_info";

    private static final String INSERT_SQL = String.format(
        "INSERT INTO `%s` (`trans_id`, `branch_id`, `log_info`, `gmt_create`, `gmt_modified`, `status`, `instance_id`) VALUES(?,?,?,now(),now(),?,?)",
        DTS_BRANCH_INFO_TABLE);

    private static final String SELECT_SQL =
        String.format("select * from `%s` where status = 0 and `branch_id`= ? and `trans_id`=? order by id desc",
            DTS_BRANCH_INFO_TABLE);

    private static final String DELETE_SQL =
        String.format("delete from `%s` where status = 0 and `branch_id`= ? and `trans_id`=?", DTS_BRANCH_INFO_TABLE);

    public static void backup(final Connection conn, final DbRuntimeContext runTimeContext) throws SQLException {
        PreparedStatement pstmt = null;
        long transId = runTimeContext.getTransId();
        long branchId = runTimeContext.getBranchId();
        String instanceId = runTimeContext.getInstanceId();
        int status = runTimeContext.getStatus();
        try {
            pstmt = conn.prepareStatement(INSERT_SQL);
            pstmt.setLong(1, transId);
            pstmt.setLong(2, branchId);
            pstmt.setBlob(3, BlobUtils.string2blob(runTimeContext.encode()));
            pstmt.setInt(4, status);
            pstmt.setString(5, instanceId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
    }

    public static void commit(final long transId, final long branchId, final String uniqueDbId) throws SQLException {
        Connection connection = getDbConnection(uniqueDbId);
        PreparedStatement pstmt = null;
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            if (originalAutoCommit) {
                connection.setAutoCommit(false);
            }
            pstmt = connection.prepareStatement(DELETE_SQL);
            pstmt.setLong(1, branchId);
            pstmt.setLong(2, transId);
            pstmt.executeUpdate();
            ResourceRowLockHelper.deleteRowLock(connection, transId, branchId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            if (originalAutoCommit) {
                connection.setAutoCommit(true);
            }
            if (pstmt != null)
                pstmt.close();
            if (connection != null)
                connection.close();
        }
    }

    private static boolean doRollback(Connection connection, CommitInfo commitInfo) throws SQLException {
        if (commitInfo.getSqlType() == SQLType.UPDATE) {
            return UpdateRollback.getInstance().rollback(commitInfo, connection);
        }
        if (commitInfo.getSqlType() == SQLType.INSERT) {
            return InsertRollback.getInstance().rollback(commitInfo, connection);
        }
        if (commitInfo.getSqlType() == SQLType.DELETE) {
            return DeleteRollback.getInstance().rollback(commitInfo, connection);
        }
        if (commitInfo.getSqlType() == SQLType.SELECT) {
            return true;
        }
        return false;
    }

    private static Connection getDbConnection(final String uniqueDbId) throws SQLException {
        DataSource dataSource = DBNAME_DATASOURCE_CACHE.get(uniqueDbId);
        if (dataSource == null) {
            throw new DtsException(
                "Datasource do not exist,may be remote service have not receive transId,so branch have not register");
        }
        Connection connection = dataSource.getConnection();
        if (connection instanceof ConnectionAdapter) {
            ConnectionAdapter adapter = (ConnectionAdapter)connection;
            return adapter.getSourceConnection();
        }
        return connection;
    }

    public static boolean isDbConnectionExist(final String uniqueDbId) throws SQLException {
        return !Objects.isNull(DBNAME_DATASOURCE_CACHE.get(uniqueDbId));
    }

    public static void registerDataSource(String uniqueDbId, DataSource dataSource) {
        DBNAME_DATASOURCE_CACHE.put(uniqueDbId, dataSource);
    }

    private static boolean rollback(Connection connection, DbRuntimeContext context) throws SQLException {
        boolean allSuccess = false;
        List<CommitInfo> commitInfoList = context.getInfo();
        if (CollectionUtils.isEmpty(commitInfoList)) {
            allSuccess = true;
        } else if (commitInfoList.size() > 1) {
            List<CommitInfo> commitInfoListDescOrder = Lists.newArrayList(commitInfoList);
            Collections.reverse(commitInfoListDescOrder);
            Iterator<CommitInfo> it = commitInfoListDescOrder.iterator();
            for (; it.hasNext();) {
                CommitInfo commitInfo = it.next();
                allSuccess = doRollback(connection, commitInfo);
            }
        } else {
            allSuccess = doRollback(connection, commitInfoList.get(0));
        }
        return allSuccess;

    }

    @SuppressWarnings("resource")
    public static void rollback(final long transId, final long branchId, final String uniqueDbId) throws SQLException {
        Connection connection = getDbConnection(uniqueDbId);
        PreparedStatement pstmt = null;
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            if (originalAutoCommit) {
                connection.setAutoCommit(false);
            }
            pstmt = connection.prepareStatement(SELECT_SQL);
            pstmt.setLong(1, branchId);
            pstmt.setLong(2, transId);
            ResultSet resultSet = pstmt.executeQuery();
            List<DbRuntimeContext> contexts = Lists.newArrayList();
            while (resultSet.next()) {
                Blob blob = resultSet.getBlob("log_info");
                String str = BlobUtils.blob2string(blob);
                contexts.add(DbRuntimeContext.decode(str));
            }
            if (contexts.size() > 1) {
                throw new SQLException("branch has to many Context,branchId:" + branchId + " transId:" + transId);
            }
            if (contexts.size() == 0) {
                ResourceRowLockHelper.deleteRowLock(connection, transId, branchId);
            } else {
                DbRuntimeContext context = contexts.get(0);
                boolean success = rollback(connection, context);
                if (success) {
                    pstmt = connection.prepareStatement(DELETE_SQL);
                    pstmt.setLong(1, branchId);
                    pstmt.setLong(2, transId);
                    pstmt.executeUpdate();
                    ResourceRowLockHelper.deleteRowLock(connection, transId, branchId);
                } else {
                    throw new SQLException("Failed to rollback data");
                }
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            if (originalAutoCommit) {
                connection.setAutoCommit(true);
            }
            if (pstmt != null)
                pstmt.close();
            if (connection != null)
                connection.close();
        }
    }
}
