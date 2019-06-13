package com.bkjk.platform.dts.server.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.bkjk.platform.dts.server.model.BranchLog;
import com.bkjk.platform.dts.server.model.GlobalLog;

@Repository
public class TransactionLogStorage {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void deleteBranchLog(long branchId, int state) {
        jdbcTemplate.update("update dts_branch_record set state = ? where branch_id = ?",
            new Object[] {state, branchId});
    }

    public void deleteGlobalLog(long transId, int state) {
        jdbcTemplate.update("update dts_global_record set state =?, gmt_modified= now() where trans_id = ?",
            new Object[] {state, transId});
    }

    public List<BranchLog> findWaitNotifyErrorLog(int commit_type) {
        return jdbcTemplate.query("select * from dts_branch_error_log where is_notify<>1 order by resource_ip",
            new Object[] {commit_type}, new RowMapper<BranchLog>() {

                @Override
                public BranchLog mapRow(ResultSet rs, int rowNum) throws SQLException {
                    BranchLog log = new BranchLog();
                    log.setTransId(rs.getLong("trans_id"));
                    log.setState(rs.getInt("state"));
                    log.setResourceIp(rs.getString("resource_ip"));
                    log.setResourceInfo(rs.getString("resource_info"));
                    log.setBranchId(rs.getLong("branch_id"));
                    log.setGmtCreated(rs.getTimestamp("gmt_created"));
                    log.setGmtModified(rs.getTimestamp("gmt_modified"));
                    return log;
                }
            });
    }

    public BranchLog getBranchLog(long branchId) {
        return jdbcTemplate.query("select * from dts_branch_record where branch_id = ?", new Object[] {branchId},
            new ResultSetExtractor<BranchLog>() {
                @Override
                public BranchLog extractData(ResultSet rs) throws SQLException, DataAccessException {
                    if (!rs.next()) {
                        return null;
                    }
                    return rowToObject(rs);
                }
            });
    }

    public List<BranchLog> getBranchLogs() {
        return jdbcTemplate.query("select * from dts_branch_record", new Object[] {}, new RowMapper<BranchLog>() {

            @Override
            public BranchLog mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rowToObject(rs);
            }
        });
    }

    public List<BranchLog> getBranchLogs(long transId) {
        return jdbcTemplate.query("select * from dts_branch_record where trans_id = ?", new Object[] {transId},
            new RowMapper<BranchLog>() {

                @Override
                public BranchLog mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rowToObject(rs);
                }
            });
    }

    public GlobalLog getGlobalLog(long transId) {
        return jdbcTemplate.query("select * from dts_global_record where trans_id = ?", new Object[] {transId},
            new ResultSetExtractor<GlobalLog>() {
                @Override
                public GlobalLog extractData(ResultSet rs) throws SQLException, DataAccessException {
                    if (!rs.next()) {
                        return null;
                    }
                    GlobalLog log = new GlobalLog();
                    log.setTransId(rs.getLong("trans_id"));
                    log.setState(rs.getInt("state"));
                    log.setGmtCreated(rs.getTimestamp("gmt_created"));
                    log.setGmtModified(rs.getTimestamp("gmt_modified"));
                    log.setClientInfo(rs.getString("client_info"));
                    log.setClientIp(rs.getString("client_ip"));
                    return log;
                }
            });
    }

    public List<GlobalLog> getGlobalLogs() {
        return jdbcTemplate.query("select * from dts_global_record", new Object[] {}, new RowMapper<GlobalLog>() {

            @Override
            public GlobalLog mapRow(ResultSet rs, int rowNum) throws SQLException {
                GlobalLog log = new GlobalLog();
                log.setTransId(rs.getLong("trans_id"));
                log.setState(rs.getInt("state"));
                log.setGmtCreated(rs.getTimestamp("gmt_created"));
                log.setGmtModified(rs.getTimestamp("gmt_modified"));
                log.setClientInfo(rs.getString("client_info"));
                log.setClientIp(rs.getString("client_ip"));
                return log;
            }
        });
    }

    public void insertBranchErrorLog(final BranchLog branchLog) {
        jdbcTemplate.update(
            "insert into dts_branch_error_log (branch_id,trans_id,state,resource_ip,resource_info,gmt_created,gmt_modified) values (?,?,?,?,?,now(),now())",
            new Object[] {branchLog.getBranchId(), branchLog.getTransId(), branchLog.getState(),
                branchLog.getResourceIp(), branchLog.getResourceInfo()});
    }

    public void insertBranchLog(BranchLog branchLog) {
        PreparedStatementCreator psc = new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(
                    "insert into dts_branch_record (trans_id,state,resource_ip,resource_info,gmt_created,gmt_modified)"
                        + " values (?,?,?,?,now(),now())",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, branchLog.getTransId());
                ps.setInt(2, branchLog.getState());
                ps.setString(3, branchLog.getResourceIp());
                ps.setString(4, branchLog.getResourceInfo());
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(psc, keyHolder);
        long branchId = keyHolder.getKey().longValue();
        branchLog.setBranchId(branchId);
        branchLog.setGmtCreated(Calendar.getInstance().getTime());
        branchLog.setGmtModified(Calendar.getInstance().getTime());
    }

    public void insertGlobalLog(final GlobalLog globalLog) {
        PreparedStatementCreator psc = new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(
                    "insert into dts_global_record (state,client_info,client_ip,gmt_created,gmt_modified) values (?,?,?,now(),now())",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, globalLog.getState());
                ps.setString(2, globalLog.getClientInfo());
                ps.setString(3, globalLog.getClientIp());
                return ps;
            }
        };
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(psc, keyHolder);
        long txId = keyHolder.getKey().longValue();
        globalLog.setTransId(txId);
        globalLog.setGmtCreated(Calendar.getInstance().getTime());
        globalLog.setGmtModified(Calendar.getInstance().getTime());
    }

    private BranchLog rowToObject(ResultSet rs) throws SQLException {
        BranchLog log = new BranchLog();
        log.setBranchId(rs.getLong("branch_id"));
        log.setTransId(rs.getLong("trans_id"));
        log.setState(rs.getInt("state"));
        log.setResourceIp(rs.getString("resource_ip"));
        log.setResourceInfo(rs.getString("resource_info"));
        log.setGmtCreated(rs.getTimestamp("gmt_created"));
        log.setGmtModified(rs.getTimestamp("gmt_modified"));
        return log;
    }

    public void updateBranchErrorLog(BranchLog branchLog) {
        jdbcTemplate.update(
            "update dts_branch_error_log set trans_id=?,state=?,resource_ip=?,resource_info=?,gmt_modified=now(),is_notify= ? where branch_id=?",
            new Object[] {branchLog.getTransId(), branchLog.getState(), branchLog.getResourceIp(),
                branchLog.getResourceInfo(), branchLog.getIsNotify(), branchLog.getBranchId()});
    }

    public void updateBranchLog(BranchLog branchLog) {
        jdbcTemplate.update("update dts_branch_record set state = ? where branch_id = ?",
            new Object[] {branchLog.getState(), branchLog.getBranchId()});
    }

    public void updateBranchState(BranchLog branchLog) {
        jdbcTemplate.update("update dts_branch_record set state = ? where branch_id = ?",
            new Object[] {branchLog.getState(), branchLog.getBranchId()});
    }

    public void updateGlobalLog(GlobalLog globalLog) {
        jdbcTemplate.update("update dts_global_record set state =?, gmt_modified= now() where trans_id = ?",
            new Object[] {globalLog.getState(), globalLog.getTransId()});
    }

    public List<GlobalLog> getGlobalTransactionToRetry(int state, int batchSize) {
        final List<GlobalLog> results = Lists.newArrayList();
        jdbcTemplate.query("select * from dts_global_record where state = ? limit ? ", new Object[] {state, batchSize},
            new ResultSetExtractor<Void>() {
                public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
                    while (rs.next()) {
                        GlobalLog log = new GlobalLog();
                        log.setTransId(rs.getLong("trans_id"));
                        log.setState(rs.getInt("state"));
                        log.setGmtCreated(rs.getTimestamp("gmt_created"));
                        log.setGmtModified(rs.getTimestamp("gmt_modified"));
                        log.setClientInfo(rs.getString("client_info"));
                        log.setClientIp(rs.getString("client_ip"));
                        results.add(log);
                    }
                    return null;
                }
            });
        results.parallelStream().forEach((globalLog) -> {
            jdbcTemplate.query("select * from dts_branch_record where trans_id = ? ",
                new Object[] {globalLog.getTransId()}, new ResultSetExtractor<Void>() {
                    public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
                        while (rs.next()) {
                            BranchLog log = new BranchLog();
                            log.setTransId(rs.getLong("trans_id"));
                            log.setState(rs.getInt("state"));
                            log.setResourceIp(rs.getString("resource_ip"));
                            log.setResourceInfo(rs.getString("resource_info"));
                            log.setBranchId(rs.getLong("branch_id"));
                            log.setGmtCreated(rs.getTimestamp("gmt_created"));
                            log.setGmtModified(rs.getTimestamp("gmt_modified"));
                            if (Objects.isNull(globalLog.getBranchLogs())) {
                                globalLog.setBranchLogs(Lists.newArrayList());
                            }
                            globalLog.getBranchLogs().add(log);
                        }
                        return null;
                    }
                });
        });
        return results;
    }

}
