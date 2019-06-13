
package com.bkjk.platform.dts.server.remoting.channel;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bkjk.platform.dts.common.protocol.RequestMessage;
import com.bkjk.platform.dts.common.protocol.header.ResourceInfoMessage;
import com.bkjk.platform.dts.common.utils.NetWorkUtil;
import com.bkjk.platform.dts.remoting.common.RemotingHelper;
import com.bkjk.platform.dts.remoting.common.RemotingUtil;
import com.google.common.collect.Maps;
import com.hazelcast.com.eclipsesource.json.Json;
import com.hazelcast.com.eclipsesource.json.JsonObject;

import io.netty.channel.Channel;

@Component
public class ChannelRepository {

    private static final Logger log = LoggerFactory.getLogger(ChannelRepository.class);

    @Value("${dts.lockTimeoutMillis:3000}")
    private long LockTimeoutMillis;

    @Value("${dts.channelExpiredTimeoutMillis:120000}")
    private long ChannelExpiredTimeout;

    private final Lock groupChannelLock = new ReentrantLock();

    private final Map<Channel, ChannelInfo> channelTable = Maps.newConcurrentMap();

    public void doChannelCloseEvent(final String remoteAddr, final Channel channel) {
        if (channel != null) {
            try {
                if (this.groupChannelLock.tryLock(LockTimeoutMillis, TimeUnit.MILLISECONDS)) {
                    try {
                        final ChannelInfo clientChannelInfo = channelTable.remove(channel);
                        if (clientChannelInfo != null) {
                            log.info("NETTY EVENT: remove channel[{}][{}] from ClientManager groupChannelTable",
                                clientChannelInfo.toString(), remoteAddr);
                        }
                    } finally {
                        this.groupChannelLock.unlock();
                    }
                } else {
                    log.warn("ClientManager doChannelCloseEvent lock timeout");
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public Channel getChannelByAddress(String address, RequestMessage msg) {
        Iterator<Map.Entry<Channel, ChannelInfo>> it = channelTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Channel, ChannelInfo> item = it.next();
            final Channel channel = item.getKey();
            final String clientIp = NetWorkUtil.toStringAddress(channel.remoteAddress());
            if (clientIp.equals(address)) {
                return channel;
            }
        }
        return getChannelByAppName(msg);

    }

    private Channel getChannelByAppName(RequestMessage msg) {
        String resourceInfo = null;
        if (Objects.isNull(msg) || !(msg instanceof ResourceInfoMessage)
            || StringUtils.isEmpty(resourceInfo = ((ResourceInfoMessage)msg).getResourceInfo())) {
            return null;
        }
        try {
            String appName = null;
            if (Objects.isNull(appName = Json.parse(resourceInfo).asObject().getString("appName", null))) {
                return null;
            }
            Iterator<Map.Entry<Channel, ChannelInfo>> it = channelTable.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Channel, ChannelInfo> item = it.next();
                final Channel channel = item.getKey();
                final String clientOrResourceInfo = item.getValue().getClientOrResourceInfo();
                if (StringUtils.isEmpty(clientOrResourceInfo)) {
                    continue;
                }
                JsonObject clientOrResourceJsonObj = Json.parse(clientOrResourceInfo).asObject();
                if (Objects.equals(appName, clientOrResourceJsonObj.getString("appName", null))
                    && !clientOrResourceJsonObj.getBoolean("fromClient", false)) {
                    return channel;
                }
            }
        } catch (Exception e) {
            log.warn(String.format("get channel for resource %s", resourceInfo), e);
        }
        return null;
    }

    public void registerChannel(final ChannelInfo clientChannelInfo) {
        try {
            ChannelInfo clientChannelInfoFound = null;
            if (this.groupChannelLock.tryLock(LockTimeoutMillis, TimeUnit.MILLISECONDS)) {
                try {
                    clientChannelInfoFound = channelTable.get(clientChannelInfo.getChannel());
                    if (null == clientChannelInfoFound) {
                        channelTable.put(clientChannelInfo.getChannel(), clientChannelInfo);
                        log.info("new producer connected, channel: {}", clientChannelInfo.toString());
                    }
                } finally {
                    this.groupChannelLock.unlock();
                }
                if (clientChannelInfoFound != null) {
                    clientChannelInfoFound.setLastUpdateTimestamp(System.currentTimeMillis());
                }
            } else {
                log.warn("ProducerManager registerProducer lock timeout");
            }
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    public void scanNotActiveChannel() {
        try {
            if (this.groupChannelLock.tryLock(LockTimeoutMillis, TimeUnit.MILLISECONDS)) {
                try {
                    Iterator<Map.Entry<Channel, ChannelInfo>> it = channelTable.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Channel, ChannelInfo> item = it.next();
                        final ChannelInfo info = item.getValue();
                        long diff = System.currentTimeMillis() - info.getLastUpdateTimestamp();
                        if (diff > ChannelExpiredTimeout) {
                            it.remove();
                            log.warn("SCAN: remove expired channel[{}] from ClientManager ",
                                RemotingHelper.parseChannelRemoteAddr(info.getChannel()));
                            RemotingUtil.closeChannel(info.getChannel());
                        }
                    }
                } finally {
                    this.groupChannelLock.unlock();
                }
            } else {
                log.warn("ClientManager scanNotActiveChannel lock timeout");
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void unregisterChannel(final String group, final ChannelInfo clientChannelInfo) {
        try {
            if (this.groupChannelLock.tryLock(LockTimeoutMillis, TimeUnit.MILLISECONDS)) {
                try {
                    ChannelInfo old = channelTable.remove(clientChannelInfo.getChannel());
                    if (old != null) {
                        log.info("unregister a producer[{}] from groupChannelTable {}", group,
                            clientChannelInfo.toString());
                    }
                } finally {
                    this.groupChannelLock.unlock();
                }
            } else {
                log.warn("ProducerManager unregisterProducer lock timeout");
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
