
package com.bkjk.platform.dts.server.remoting.channel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bkjk.platform.dts.remoting.ChannelEventListener;
import com.hazelcast.internal.util.concurrent.ThreadFactoryImpl;

import io.netty.channel.Channel;

@Component
public class ChannelkeepingComponent implements ChannelEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ChannelkeepingComponent.class);

    @Autowired
    private ChannelRepository channelRepository;

    private final ScheduledExecutorService scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ClientHousekeepingScheduledThread"));

    @Override
    public void onChannelClose(String remoteAddr, Channel channel) {
        channelRepository.doChannelCloseEvent(remoteAddr, channel);
    }

    @Override
    public void onChannelConnect(String remoteAddr, Channel channel) {
    }

    @Override
    public void onChannelException(String remoteAddr, Channel channel) {
        channelRepository.doChannelCloseEvent(remoteAddr, channel);
    }

    @Override
    public void onChannelIdle(String remoteAddr, Channel channel) {
        channelRepository.doChannelCloseEvent(remoteAddr, channel);
    }

    public void start() {
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    channelRepository.scanNotActiveChannel();
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }, 1000 * 10, 1000 * 10, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        this.scheduledExecutorService.shutdown();
    }

}
