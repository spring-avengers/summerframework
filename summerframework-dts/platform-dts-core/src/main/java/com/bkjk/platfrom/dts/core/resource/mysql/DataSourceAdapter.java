package com.bkjk.platfrom.dts.core.resource.mysql;

import com.bkjk.platform.dts.common.DtsContext;
import com.bkjk.platform.dts.common.DtsException;
import com.bkjk.platform.dts.common.DtsXID;
import com.bkjk.platform.dts.common.model.ResourceInfo;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchMessage;
import com.bkjk.platform.dts.common.protocol.header.RegisterBranchResultMessage;
import com.bkjk.platform.eureka.util.JsonUtil;
import com.bkjk.platfrom.dts.core.resource.DefaultDtsResourcMessageSender;
import com.bkjk.platfrom.dts.core.resource.DtsResourceManager;
import com.bkjk.platfrom.dts.core.resource.mysql.common.TableMetaUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class DataSourceAdapter implements DataSource, DtsResourceManager {

    private final DataSource dataSource;

    private final String appName;

    private final String dbName;

    private final DefaultDtsResourcMessageSender defaultDtsResourcMessageSender = new DefaultDtsResourcMessageSender();

    public DataSourceAdapter(final String appName, final DataSource dataSource) throws SQLException {
        String dbName = TableMetaUtils.getDbNameFromUrl(dataSource.getConnection().getMetaData().getURL());
        this.dataSource = dataSource;
        this.appName = appName;
        this.dbName = dbName;
        defaultDtsResourcMessageSender.registerResourceManager(this);
        defaultDtsResourcMessageSender.start();
    }

    @Override
    public void branchCommit(long tranId, long branchId) throws DtsException {
        try {
            if (!ResourceRedoUndoHelper.isDbConnectionExist(getUniqueDbId())) {
                // In case of dts-server retry after app restart
                ResourceRedoUndoHelper.registerDataSource(getUniqueDbId(), this);
            }
            ResourceRedoUndoHelper.commit(tranId, branchId, getUniqueDbId());
        } catch (SQLException e) {
            throw new DtsException(e);
        }
    }

    @Override
    public void branchRollback(long tranId, long branchId) throws DtsException {
        try {
            if (!ResourceRedoUndoHelper.isDbConnectionExist(getUniqueDbId())) {
                // In case of dts-server retry after app restart
                ResourceRedoUndoHelper.registerDataSource(getUniqueDbId(), this);
            }
            ResourceRedoUndoHelper.rollback(tranId, branchId, getUniqueDbId());
        } catch (SQLException e) {
            throw new DtsException(e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = this.dataSource.getConnection();
        return new ConnectionAdapter(this, connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = this.dataSource.getConnection(username, password);
        return new ConnectionAdapter(this, connection);
    }

    public DtsResourceManager getDtsResourceManager() {
        return this;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.dataSource.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.dataSource.getLogWriter();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.dataSource.getParentLogger();
    }

    public DataSource getSourceDataSource() {
        return this.dataSource;
    }

    public String getUniqueDbId() {
        return appName + "_" + dbName;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.dataSource.isWrapperFor(iface);
    }

    @Override
    public long register() throws DtsException {
        RegisterBranchMessage registerMessage = new RegisterBranchMessage();
        ResourceInfo resourceInfo = new ResourceInfo();
        resourceInfo.setAppName(appName);
        resourceInfo.setDbName(dbName);
        registerMessage.setResourceInfo(JsonUtil.toJson(resourceInfo));
        registerMessage.setTranId(DtsXID.getTransactionId(DtsContext.getInstance().getCurrentXid()));
        RegisterBranchResultMessage resultMessage =
            (RegisterBranchResultMessage)defaultDtsResourcMessageSender.invoke(registerMessage);
        if (resultMessage == null) {
            throw new DtsException("register resourcemanager failed,");
        } else {
            return resultMessage.getBranchId();
        }
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.dataSource.setLoginTimeout(seconds);
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.dataSource.setLogWriter(out);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.dataSource.unwrap(iface);
    }

}
