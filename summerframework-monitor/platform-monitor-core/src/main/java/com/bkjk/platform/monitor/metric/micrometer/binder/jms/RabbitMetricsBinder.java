package com.bkjk.platform.monitor.metric.micrometer.binder.jms;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MetricsCollector;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.Nullable;

public class RabbitMetricsBinder implements MeterBinder, MetricsCollector {
    private static class ChannelState {

        final Lock lock = new ReentrantLock();

        final Set<Long> unackedMessageDeliveryTags = new HashSet<Long>();
        final Set<String> consumersWithManualAck = new HashSet<String>();

        final Channel channel;

        private ChannelState(Channel channel) {
            this.channel = channel;
        }

    }

    private static class ConnectionState {

        final ConcurrentMap<Integer, RabbitMetricsBinder.ChannelState> channelState =
            new ConcurrentHashMap<Integer, RabbitMetricsBinder.ChannelState>();
        final Connection connection;

        private ConnectionState(Connection connection) {
            this.connection = connection;
        }
    }

    private static final String JMX_DOMAIN = CachingConnectionFactory.class.getPackage().getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMetricsBinder.class);

    private static MBeanServer getMBeanServer() {
        List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (!mBeanServers.isEmpty()) {
            return mBeanServers.get(0);
        }
        return ManagementFactory.getPlatformMBeanServer();
    }

    private final MBeanServer mBeanServer;

    private final Iterable<Tag> tags;

    private final ConcurrentMap<String, RabbitMetricsBinder.ConnectionState> connectionState =
        new ConcurrentHashMap<String, RabbitMetricsBinder.ConnectionState>();

    private AtomicInteger connections = null;

    private AtomicInteger channels = null;

    private AtomicInteger publishedMessages = null;

    private AtomicInteger consumedMessages = null;
    private AtomicInteger acknowledgedMessages = null;
    private AtomicInteger rejectedMessages = null;

    public RabbitMetricsBinder(AbstractConnectionFactory connectionFactory, Iterable<Tag> tags) {
        this.mBeanServer = getMBeanServer();
        this.tags = tags;
        com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = connectionFactory.getRabbitConnectionFactory();
        String metricsPrefix = "rabbit";

        this.connections = Metrics.gauge((metricsPrefix + ".connections"), new AtomicInteger(0));
        this.channels = Metrics.gauge((metricsPrefix + ".channels"), new AtomicInteger(0));
        this.publishedMessages = Metrics.gauge((metricsPrefix + ".published"), new AtomicInteger(0));
        this.consumedMessages = Metrics.gauge((metricsPrefix + ".consumed"), new AtomicInteger(0));
        this.acknowledgedMessages = Metrics.gauge((metricsPrefix + ".acknowledged"), new AtomicInteger(0));
        this.rejectedMessages = Metrics.gauge((metricsPrefix + ".rejected"), new AtomicInteger(0));

        rabbitConnectionFactory.setMetricsCollector(this);
    }

    @Override
    public void basicAck(Channel channel, long deliveryTag, boolean multiple) {
        try {
            updateChannelStateAfterAckReject(channel, deliveryTag, multiple, acknowledgedMessages);
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in basicAck: " + e.getMessage());
        }
    }

    @Override
    public void basicCancel(Channel channel, String consumerTag) {
        try {
            RabbitMetricsBinder.ChannelState channelState = channelState(channel);
            channelState.lock.lock();
            try {
                channelState(channel).consumersWithManualAck.remove(consumerTag);
            } finally {
                channelState.lock.unlock();
            }
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in basicCancel: " + e.getMessage());
        }
    }

    @Override
    public void basicConsume(Channel channel, String consumerTag, boolean autoAck) {
        try {
            if (!autoAck) {
                RabbitMetricsBinder.ChannelState channelState = channelState(channel);
                channelState.lock.lock();
                try {
                    channelState(channel).consumersWithManualAck.add(consumerTag);
                } finally {
                    channelState.lock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in basicConsume: " + e.getMessage());
        }
    }

    @Override
    public void basicNack(Channel channel, long deliveryTag) {
        try {
            updateChannelStateAfterAckReject(channel, deliveryTag, true, rejectedMessages);
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in basicNack: " + e.getMessage());
        }
    }

    @Override
    public void basicPublish(Channel channel) {
        try {
            publishedMessages.incrementAndGet();
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in basicPublish: " + e.getMessage());
        }
    }

    @Override
    public void basicReject(Channel channel, long deliveryTag) {
        try {
            updateChannelStateAfterAckReject(channel, deliveryTag, false, rejectedMessages);
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in basicReject: " + e.getMessage());
        }
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerMetricsEventually("CachingConnectionFactory", (o, tags) -> {
            registerGaugeForObject(registry, o, "idleChannelsTxHighWater",
                "rabbit_cacheConnectionProperties_idleChannelsTxHighWater", tags, "", "");
            registerGaugeForObject(registry, o, "idleChannelsTx", "rabbit_cacheConnectionProperties_idleChannelsTx",
                tags, "", "");
            registerGaugeForObject(registry, o, "channelCacheSize", "rabbit_cacheConnectionProperties_channelCacheSize",
                tags, "", "");
            registerGaugeForObject(registry, o, "idleChannelsNotTx",
                "rabbit_cacheConnectionProperties_idleChannelsNotTx", tags, "", "");
            registerGaugeForObject(registry, o, "idleChannelsNotTxHighWater",
                "rabbit_cacheConnectionProperties_idleChannelsNotTxHighWater", tags, "", "");
        });
    }

    private RabbitMetricsBinder.ChannelState channelState(Channel channel) {
        return connectionState(channel.getConnection()).channelState.get(channel.getChannelNumber());
    }

    public void cleanStaleState() {
        try {
            Iterator<Map.Entry<String, RabbitMetricsBinder.ConnectionState>> connectionStateIterator =
                connectionState.entrySet().iterator();
            while (connectionStateIterator.hasNext()) {
                Map.Entry<String, RabbitMetricsBinder.ConnectionState> connectionEntry = connectionStateIterator.next();
                Connection connection = connectionEntry.getValue().connection;
                if (connection.isOpen()) {
                    Iterator<Map.Entry<Integer, RabbitMetricsBinder.ChannelState>> channelStateIterator =
                        connectionEntry.getValue().channelState.entrySet().iterator();
                    while (channelStateIterator.hasNext()) {
                        Map.Entry<Integer, RabbitMetricsBinder.ChannelState> channelStateEntry =
                            channelStateIterator.next();
                        Channel channel = channelStateEntry.getValue().channel;
                        if (!channel.isOpen()) {
                            channelStateIterator.remove();
                            channels.decrementAndGet();
                            LOGGER.info(
                                "Ripped off state of channel {} of connection {}. This is abnormal, please report.",
                                channel.getChannelNumber(), connection.getId());
                        }
                    }
                } else {
                    connectionStateIterator.remove();
                    connections.decrementAndGet();
                    IntStream.range(0, connectionEntry.getValue().channelState.size())
                        .forEach((e) -> channels.decrementAndGet());
                    LOGGER.info("Ripped off state of connection {}. This is abnormal, please report.",
                        connection.getId());
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error during periodic clean of metricsCollector: " + e.getMessage());
        }
    }

    @Override
    public void closeChannel(Channel channel) {
        try {
            RabbitMetricsBinder.ChannelState removed =
                connectionState(channel.getConnection()).channelState.remove(channel.getChannelNumber());
            if (removed != null) {
                channels.decrementAndGet();
            }
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in closeChannel: " + e.getMessage());
        }
    }

    @Override
    public void closeConnection(Connection connection) {
        try {
            RabbitMetricsBinder.ConnectionState removed = connectionState.remove(connection.getId());
            if (removed != null) {
                connections.decrementAndGet();
            }
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in closeConnection: " + e.getMessage());
        }
    }

    private RabbitMetricsBinder.ConnectionState connectionState(Connection connection) {
        return connectionState.get(connection.getId());
    }

    @Override
    public void consumedMessage(Channel channel, long deliveryTag, boolean autoAck) {
        try {
            consumedMessages.incrementAndGet();
            if (!autoAck) {
                RabbitMetricsBinder.ChannelState channelState = channelState(channel);
                channelState.lock.lock();
                try {
                    channelState(channel).unackedMessageDeliveryTags.add(deliveryTag);
                } finally {
                    channelState.lock.unlock();
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in consumedMessage: " + e.getMessage());
        }
    }

    @Override
    public void consumedMessage(Channel channel, long deliveryTag, String consumerTag) {
        try {
            consumedMessages.incrementAndGet();
            RabbitMetricsBinder.ChannelState channelState = channelState(channel);
            channelState.lock.lock();
            try {
                if (channelState.consumersWithManualAck.contains(consumerTag)) {
                    channelState.unackedMessageDeliveryTags.add(deliveryTag);
                }
            } finally {
                channelState.lock.unlock();
            }
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in consumedMessage: " + e.getMessage());
        }
    }

    public Meter getAcknowledgedMessages() {
        return null;
    }

    public Counter getChannels() {
        return null;
    }

    public Counter getConnections() {
        return null;
    }

    public Meter getConsumedMessages() {
        return null;
    }

    public Meter getPublishedMessages() {
        return null;
    }

    public Meter getRejectedMessages() {
        return null;
    }

    private double getValue(MBeanServer mBeanServer, ObjectName objectName, String attribute) {
        try {
            Properties cacheProperties = (Properties)mBeanServer.getAttribute(objectName, "CacheProperties");
            return Double.parseDouble(cacheProperties.getProperty(attribute));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    @Override
    public void newChannel(final Channel channel) {
        try {
            channels.incrementAndGet();
            channel.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    closeChannel(channel);
                }
            });
            connectionState(channel.getConnection()).channelState.put(channel.getChannelNumber(),
                new RabbitMetricsBinder.ChannelState(channel));
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in newChannel: " + e.getMessage());
        }
    }

    @Override
    public void newConnection(final Connection connection) {
        try {
            if (connection.getId() == null) {
                connection.setId(UUID.randomUUID().toString());
            }
            connections.incrementAndGet();
            connectionState.put(connection.getId(), new RabbitMetricsBinder.ConnectionState(connection));
            connection.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    closeConnection(connection);
                }
            });
        } catch (Exception e) {
            LOGGER.info("Error while computing metrics in newConnection: " + e.getMessage());
        }
    }

    private void registerGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, String meterName,
        Tags allTags, String description, @Nullable String baseUnit) {
        Gauge.builder(meterName, getMBeanServer(), s -> getValue(s, o, jmxMetricName)).description(description)
            .baseUnit(baseUnit).tags(tags).register(registry);
    }

    private void registerMetricsEventually(String type, BiConsumer<ObjectName, Tags> perObject) {
        try {
            Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName(JMX_DOMAIN + ":type=" + type + ",*"), null);
            if (!objs.isEmpty()) {
                for (ObjectName o : objs) {
                    perObject.accept(o, Tags.of(tags));
                }
                return;
            }
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Error registering Rabbit JMX based metrics", e);
        }

        NotificationListener notificationListener = new NotificationListener() {

            @Override
            public void handleNotification(Notification notification, Object handback) {
                MBeanServerNotification mbs = (MBeanServerNotification)notification;
                ObjectName o = mbs.getMBeanName();
                perObject.accept(o, Tags.of(tags));
                try {
                    mBeanServer.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this);
                } catch (InstanceNotFoundException | ListenerNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }

        };

        NotificationFilter filter = (NotificationFilter)notification -> {
            if (!MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType()))
                return false;
            ObjectName obj = ((MBeanServerNotification)notification).getMBeanName();
            return obj.getDomain().equals(JMX_DOMAIN) && obj.getKeyProperty("type").equals(type);
        };

        try {
            mBeanServer.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter, null);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("Error registering Rabbit MBean listener", e);
        }
    }

    private void updateChannelStateAfterAckReject(Channel channel, long deliveryTag, boolean multiple,
        AtomicInteger meter) {
        RabbitMetricsBinder.ChannelState channelState = channelState(channel);
        channelState.lock.lock();
        try {
            if (multiple) {
                Iterator<Long> iterator = channelState.unackedMessageDeliveryTags.iterator();
                while (iterator.hasNext()) {
                    long messageDeliveryTag = iterator.next();
                    if (messageDeliveryTag <= deliveryTag) {
                        iterator.remove();
                        meter.incrementAndGet();
                    }
                }
            } else {
                channelState.unackedMessageDeliveryTags.remove(deliveryTag);
                meter.incrementAndGet();
            }
        } finally {
            channelState.lock.unlock();
        }
    }
}
