package com.bkjk.platform.dts.ops.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("dts_global_record")
public class GlobalRecord {
    @TableField("trans_id")
    private Long transId;
    private int state;
    @TableField("gmt_created")
    private Date createdTime;
    @TableField("gmt_modified")
    private Date modifiedTime;

    @TableField("client_info")
    private String clientInfo;

    @TableField("client_ip")
    private String clientIp;

    public String getClientInfo() {
        return clientInfo;
    }

    public String getClientIp() {
        return clientIp;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public int getState() {
        return state;
    }

    public Long getTransId() {
        return transId;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setTransId(Long transId) {
        this.transId = transId;
    }
}
