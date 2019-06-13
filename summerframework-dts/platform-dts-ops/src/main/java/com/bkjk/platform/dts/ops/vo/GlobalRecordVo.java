package com.bkjk.platform.dts.ops.vo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GlobalRecordVo {
    private Long transId;
    private int state;
    private Date createdTime;
    private Date modifiedTime;

    private String clientInfo;

    private String clientIp;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String getClientInfo() {
        return clientInfo;
    }

    public String getClientIp() {
        return clientIp;
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

    public int getState() {
        return state;
    }

    public String getStateStr() {
        return GlobalLogState.getNameByStateValue(getState());
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
