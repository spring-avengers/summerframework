
package com.bkjk.platform.jobcenter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.jobcenter")
public class JobCenterProperties {

    private String serviceId = "soa-jobcenter";

    private String appName;

    private int port = 9998;

    private String accessToken;

    private String logPath;

    private int logRetentionDays;

    private String localIp;

    public String getAccessToken() {
        return accessToken;
    }

    public String getAppName() {
        return appName;
    }

    public String getLocalIp() {
        return localIp;
    }

    public String getLogPath() {
        return logPath;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public int getPort() {
        return port;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

}
