package com.bkjk.platform.dts.ops.vo;

import java.util.Date;

public class BranchErrorRecordVo {
    private Long branchId;
    private Long transId;
    private String resourceIp;
    private String resourceInfo;
    private int state;
    private Date createdTime;
    private Date modifiedTime;
    private boolean notified;

    public Long getBranchId() {
        return branchId;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
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

    public Long getTransId() {
        return transId;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
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

    public void setTransId(Long transId) {
        this.transId = transId;
    }
}
