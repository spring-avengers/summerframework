package com.bkjk.platfrom.dts.core.resource.mysql;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.springframework.util.CollectionUtils;

import com.bkjk.platform.dts.common.DtsContext;
import com.bkjk.platform.dts.common.DtsXID;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platfrom.dts.core.resource.DtsResourceManager;

public class ConnectionAdapter implements Connection {

    private final DataSourceAdapter dataSourceAdapter;

    private final Connection connection;

    private DbRuntimeContext dbRuntimeContext;

    public ConnectionAdapter(DataSourceAdapter dataSourceAdapter, Connection connection) {
        this.dataSourceAdapter = dataSourceAdapter;
        this.connection = connection;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        this.connection.abort(executor);
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.connection.clearWarnings();
    }

    @Override
    public void close() throws SQLException {
        try {
            this.connection.close();
        } finally {
            this.dbRuntimeContext = null;
        }
    }

    @Override
    public void commit() throws SQLException {
        try {
            if (this.inTransaction()) {
                dbRuntimeContext.setStatus(0);
                if (!CollectionUtils.isEmpty(dbRuntimeContext.getInfo())) {
                    ResourceRedoUndoHelper.backup(this.connection, dbRuntimeContext);
                }
            }
            this.connection.commit();
        } finally {
            this.dbRuntimeContext = null;
        }
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return this.connection.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return this.connection.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return this.connection.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return this.connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return this.connection.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        Statement statement = this.connection.createStatement();
        return new StatementAdapter(this, statement);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        Statement statement = this.connection.createStatement(resultSetType, resultSetConcurrency);
        return new StatementAdapter(this, statement);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        Statement statement =
            this.connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        return new StatementAdapter(this, statement);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return this.connection.createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        boolean autoCommit = this.connection.getAutoCommit();
        return autoCommit;
    }

    @Override
    public String getCatalog() throws SQLException {
        return this.connection.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return this.connection.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return this.connection.getClientInfo(name);
    }

    public DbRuntimeContext getConnectionRuntimeContext() {
        return this.dbRuntimeContext;
    }

    public DataSourceAdapter getDataSource() {
        return dataSourceAdapter;
    }

    @Override
    public int getHoldability() throws SQLException {
        return this.connection.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return this.connection.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return this.connection.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
        return this.connection.getSchema();
    }

    public Connection getSourceConnection() {
        return this.connection;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return this.connection.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return this.connection.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return this.connection.getWarnings();
    }

    private boolean inTransaction() {
        return DtsContext.getInstance().inTransaction();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.connection.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.connection.isReadOnly();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return this.connection.isValid(timeout);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.connection.isWrapperFor(iface);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return this.connection.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return this.connection.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) throws SQLException {
        return this.connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement(sql);
        return new PreparedStatementAdapter(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement(sql, autoGeneratedKeys);
        return new PreparedStatementAdapter(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException {
        PreparedStatement preparedStatement =
            this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        return new PreparedStatementAdapter(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) throws SQLException {
        PreparedStatement preparedStatement =
            this.connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        return new PreparedStatementAdapter(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement(sql, columnIndexes);
        return new PreparedStatementAdapter(this, preparedStatement, sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        PreparedStatement preparedStatement = this.connection.prepareStatement(sql, columnNames);
        return new PreparedStatementAdapter(this, preparedStatement, sql);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.connection.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        try {
            this.connection.rollback();
        } finally {
            this.dbRuntimeContext = null;
        }
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        this.connection.rollback(savepoint);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!autoCommit && inTransaction()) {
            DtsResourceManager dtsResourceManager = this.dataSourceAdapter.getDtsResourceManager();
            String uniqueDbId = this.dataSourceAdapter.getUniqueDbId();
            long branchId = dtsResourceManager.register();
            long transId = DtsXID.getTransactionId(DtsContext.getInstance().getCurrentXid());
            this.dbRuntimeContext = new DbRuntimeContext();
            dbRuntimeContext.setBranchId(branchId);
            dbRuntimeContext.setTransId(transId);
            dbRuntimeContext.setInstanceId(dataSourceAdapter.getUniqueDbId() + "_" + NetWorkUtil.getLocalIp());
            ResourceRedoUndoHelper.registerDataSource(uniqueDbId, dataSourceAdapter);
        }
        this.connection.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        this.connection.setCatalog(catalog);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.connection.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        this.connection.setClientInfo(name, value);;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.connection.setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.connection.setReadOnly(readOnly);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return this.connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return this.connection.setSavepoint(name);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        this.connection.setSchema(schema);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        this.connection.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.connection.setTypeMap(map);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.connection.unwrap(iface);
    }

}
