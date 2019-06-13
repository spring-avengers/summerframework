
package com.bkjk.platform.dts.server.remoting.channel;

import io.netty.channel.Channel;

public class ChannelInfo {
    private final Channel channel;
    private final String clientOrResourceInfo;
    private volatile long lastUpdateTimestamp = System.currentTimeMillis();

    public ChannelInfo(Channel channel, String clientOrResourceInfo) {
        this.channel = channel;
        this.clientOrResourceInfo = clientOrResourceInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChannelInfo other = (ChannelInfo)obj;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!channel.equals(other.channel))
            return false;
        if (clientOrResourceInfo == null) {
            if (other.clientOrResourceInfo != null)
                return false;
        } else if (!clientOrResourceInfo.equals(other.clientOrResourceInfo))
            return false;
        if (lastUpdateTimestamp != other.lastUpdateTimestamp)
            return false;
        return true;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getClientOrResourceInfo() {
        return clientOrResourceInfo;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : channel.hashCode());
        result = prime * result + ((clientOrResourceInfo == null) ? 0 : clientOrResourceInfo.hashCode());
        result = prime * result + (int)(lastUpdateTimestamp ^ (lastUpdateTimestamp >>> 32));
        return result;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    @Override
    public String toString() {
        return "ChannelInfo [channel=" + channel + ", clientOrResourceInfo=" + clientOrResourceInfo
            + ", lastUpdateTimestamp=" + lastUpdateTimestamp + "]";
    }

}
