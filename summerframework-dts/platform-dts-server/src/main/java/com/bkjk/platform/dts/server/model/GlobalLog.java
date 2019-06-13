package com.bkjk.platform.dts.server.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GlobalLog implements Serializable {

    private static final long serialVersionUID = 5377118251174933927L;

    private long transId;

    private int state;

    private long timeout;

    private Date gmtCreated;

    private Date gmtModified;

    private String clientInfo;

    private String clientIp;

    private List<BranchLog> branchLogs;

    private List<Long> branchIds = Collections.synchronizedList(new ArrayList<Long>());

    public List<Long> getBranchIds() {
        return branchIds;
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public int getLeftBranches() {
        return branchIds.size();
    }

    public int getState() {
        return state;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getTransId() {
        return transId;
    }

    public void setBranchIds(List<Long> branchIds) {
        this.branchIds = branchIds;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setTransId(long transId) {
        this.transId = transId;
    }

    public List<BranchLog> getBranchLogs() {
        return branchLogs;
    }

    public void setBranchLogs(List<BranchLog> branchLogs) {
        this.branchLogs = branchLogs;
    }

    @Override
    public String toString() {
        return "GlobalLog [transId=" + transId + ", state=" + state + ", timeout=" + timeout + ", gmtCreated="
            + gmtCreated + ", gmtModified=" + gmtModified + ", clientInfo=" + clientInfo + ", clientIp=" + clientIp
            + ", branchIds=" + branchIds + "]";
    }

}
