package com.bkjk.platform.dts.common.protocol.heatbeat;

public class HeartbeatClientOrResourceInfo {
    private String appName;

    private boolean fromClient;

    public String getAppName() {
        return appName;
    }

    public boolean isFromClient() {
        return fromClient;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setFromClient(boolean fromClient) {
        this.fromClient = fromClient;
    }
}
