package com.bkjk.platform.dts.server.model;

import java.io.Serializable;
import java.util.Date;

public class BranchLog implements Serializable {

    private static final long serialVersionUID = 8212281582655543794L;

    private long branchId;

    private long transId;

    private int state;

    private String resourceIp;

    private String resourceInfo;

    private Date gmtCreated;

    private Date gmtModified;

    private int isNotify;

    public long getBranchId() {
        return branchId;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public int getIsNotify() {
        return isNotify;
    }

    public String getResourceInfo() {
        return resourceInfo;
    }

    public String getResourceIp() {
        return resourceIp;
    }

    public int getState() {
        return state;
    }

    public long getTransId() {
        return transId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public void setIsNotify(int isNotify) {
        this.isNotify = isNotify;
    }

    public void setResourceInfo(String resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public void setResourceIp(String resourceIp) {
        this.resourceIp = resourceIp;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setTransId(long transId) {
        this.transId = transId;
    }

}
