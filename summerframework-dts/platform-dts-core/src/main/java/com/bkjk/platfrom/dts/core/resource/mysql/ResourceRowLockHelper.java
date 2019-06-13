package com.bkjk.platfrom.dts.core.resource.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import com.bkjk.platform.dts.common.DtsLockConflictException;
import com.bkjk.platfrom.dts.core.resource.mysql.common.PkPair;

public class ResourceRowLockHelper {

    private static final String DTS_ROW_LOCK_TABLE = "dts_row_lock";

    private static final String INSERT_SQL = String.format(
        "INSERT INTO `%s` (`branch_id`,`trans_id`,`table_name`,`row_key`,`instance_id`,`gmt_create`,`gmt_modified`) VALUES(?,?,?,?,?,now(),now())",
        DTS_ROW_LOCK_TABLE);

    private static final String SELECT_SQL =
        String.format("select * from `%s` where `table_name` = ? and `row_key` = ?", DTS_ROW_LOCK_TABLE);

    private static final String DELETE_SQL =
        String.format("delete from `%s` where `branch_id` = ? and `trans_id` = ?", DTS_ROW_LOCK_TABLE);

    public static String buildRowKey(List<PkPair<String, Object>> primaryKeyValues) {
        return primaryKeyValues.stream()
            .map(primaryKeyValue -> primaryKeyValue.getKey() + ":" + String.valueOf(primaryKeyValue.getValue()))
            .collect(Collectors.joining(","));
    }

    public static void deleteRowLock(final Connection connection, final long transId, final long branchId)
        throws SQLException {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(DELETE_SQL);
            pstmt.setLong(1, branchId);
            pstmt.setLong(2, transId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
    }

    public static void insertRowLock(final ConnectionAdapter adapterConnection, final DbRuntimeContext runTimeContext,
        final String tableName, final Object rowKey) throws SQLException {
        long transId = runTimeContext.getTransId();
        long branchId = runTimeContext.getBranchId();
        String instanceId = runTimeContext.getInstanceId();
        PreparedStatement pstmt = null;
        try {
            pstmt = adapterConnection.getSourceConnection().prepareStatement(INSERT_SQL);
            pstmt.setLong(1, branchId);
            pstmt.setLong(2, transId);
            pstmt.setString(3, tableName);
            pstmt.setObject(4, rowKey);
            pstmt.setString(5, instanceId);
            pstmt.executeUpdate();
        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                Triple<Long, Long, String> triple = query(adapterConnection.getSourceConnection(), tableName, rowKey);
                if (!Objects.isNull(triple) && !Objects.isNull(triple.getMiddle()) && transId == triple.getMiddle()) {
                    return;
                }
                throw new DtsLockConflictException(String.format(
                    "Row[Table:%s,Pk:%s] locked by other global transaction[TransId:%s,BranchId:%s,InstanceId:%s]",
                    tableName, rowKey, triple.getMiddle(), triple.getLeft(), triple.getRight()));
            } else {
                throw e;
            }
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
    }

    private static Triple<Long, Long, String> query(final Connection connection, final String tableName,
        final Object rowKey) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(SELECT_SQL);
            pstmt.setString(1, tableName);
            pstmt.setObject(2, rowKey);
            ResultSet resultSet = pstmt.executeQuery();
            Triple<Long, Long, String> triple = null;
            while (resultSet.next()) {
                Long lockedBranchId = resultSet.getLong("branch_id");
                Long lockedTransId = resultSet.getLong("trans_id");
                String lockedInstanceId = resultSet.getString("instance_id");
                triple = new MutableTriple<Long, Long, String>(lockedBranchId, lockedTransId, lockedInstanceId);
            }
            return triple;
        } catch (SQLException e) {
            throw e;
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
    }

    public static boolean rowLocked(final ConnectionAdapter adapterConnection, final DbRuntimeContext runTimeContext,
        final String tableName, final Object rowKey) throws SQLException {
        long transId = runTimeContext.getTransId();
        PreparedStatement pstmt = null;
        try {
            Triple<Long, Long, String> triple = query(adapterConnection.getSourceConnection(), tableName, rowKey);
            if (Objects.isNull(triple) || (!Objects.isNull(triple.getMiddle()) && transId == triple.getMiddle())) {

                return false;
            }
            return true;
        } finally {
            if (pstmt != null)
                pstmt.close();
        }
    }

}
