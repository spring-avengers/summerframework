
package com.bkjk.platform.common;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

@SuppressWarnings("restriction")
public class ServerLoadStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String osName;

    private double systemLoadAverage;

    private long usedMemory;

    private int availableProcessors;

    private long freePhysicalMemorySize;

    private long totalPhysicalMemorySize;

    public void calculateSystemInfo() {
        this.osName = System.getProperty("os.name");
        int kb = 1024;
        OperatingSystemMXBean osmxb = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        this.systemLoadAverage = osmxb.getSystemLoadAverage();
        this.availableProcessors = osmxb.getAvailableProcessors();
        this.freePhysicalMemorySize = osmxb.getFreePhysicalMemorySize();
        this.totalPhysicalMemorySize = osmxb.getTotalPhysicalMemorySize();
        this.usedMemory = (osmxb.getTotalPhysicalMemorySize() - osmxb.getFreePhysicalMemorySize()) / kb;

    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public long getFreePhysicalMemorySize() {
        return freePhysicalMemorySize;
    }

    public String getOsName() {
        return osName;
    }

    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    public long getTotalPhysicalMemorySize() {
        return totalPhysicalMemorySize;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public void setFreePhysicalMemorySize(long freePhysicalMemorySize) {
        this.freePhysicalMemorySize = freePhysicalMemorySize;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public void setSystemLoadAverage(double systemLoadAverage) {
        this.systemLoadAverage = systemLoadAverage;
    }

    public void setTotalPhysicalMemorySize(long totalPhysicalMemorySize) {
        this.totalPhysicalMemorySize = totalPhysicalMemorySize;
    }

    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
    }

}
