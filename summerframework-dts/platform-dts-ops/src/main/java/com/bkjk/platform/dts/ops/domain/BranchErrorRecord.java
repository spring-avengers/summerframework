package com.bkjk.platform.dts.ops.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("dts_branch_error_log")
public class BranchErrorRecord {
    @TableField("branch_id")
    private Long branchId;
    @TableField("trans_id")
    private Long transId;
    @TableField("resource_ip")
    private String resourceIp;
    @TableField("resource_info")
    private String resourceInfo;
    private int state;
    @TableField("gmt_created")
    private Date createdTime;
    @TableField("gmt_modified")
    private Date modifiedTime;
    @TableField("is_notify")
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
