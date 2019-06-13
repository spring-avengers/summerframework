package com.bkjk.platform.dts.ops.vo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BranchRecordVo {
    private Long branchId;
    private Long transId;
    private String resourceIp;
    private String resourceInfo;
    private int state;
    private Date createdTime;
    private Date modifiedTime;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Long getBranchId() {
        return branchId;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public String getCreatedTimeStr() {
        return df.format(createdTime);
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public String getModifiedTimeStr() {
        return df.format(modifiedTime);
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

    public String getStateStr() {
        return BranchLogState.getNameByStateValue(getState());
    }

    public Long getTransId() {
        return transId;
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
