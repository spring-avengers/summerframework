package com.bkjk.platform.monitor.metric.micrometer.binder.jms;

import static java.util.Collections.emptyList;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.lang.Nullable;

public class KafkaProducerMetricsBinder implements MeterBinder {
    private static final String JMX_DOMAIN = "kafka.producer";
    private static final String METRIC_NAME_PREFIX = "kafka.producer.";

    private static MBeanServer getMBeanServer() {
        List<MBeanServer> mBeanServers = MBeanServerFactory.findMBeanServer(null);
        if (!mBeanServers.isEmpty()) {
            return mBeanServers.get(0);
        }
        return ManagementFactory.getPlatformMBeanServer();
    }

    private static String sanitize(String value) {
        return value.replaceAll("-", ".");
    }

    private final Iterable<Tag> tags;

    private final MBeanServer mBeanServer;

    public KafkaProducerMetricsBinder() {
        this(emptyList());
    }

    public KafkaProducerMetricsBinder(Iterable<Tag> tags) {
        this(getMBeanServer(), tags);
    }

    public KafkaProducerMetricsBinder(MBeanServer mBeanServer, Iterable<Tag> tags) {
        this.mBeanServer = mBeanServer;
        this.tags = tags;
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        registerMetricsEventually("producer-metrics", (o, tags) -> {
            registerGaugeForObject(meterRegistry, o, "connection-count", tags,
                "The current number of active connections.", "connections");
            registerGaugeForObject(meterRegistry, o, "connections-creation-total", tags, "New connections established.",
                "connections");
            registerGaugeForObject(meterRegistry, o, "connections-close-total", tags, "Connections closed.",
                "connections");
            registerGaugeForObject(meterRegistry, o, "io-ratio", tags,
                "The fraction of time the I/O thread spent doing I/O.", null);
            registerGaugeForObject(meterRegistry, o, "select-total", tags,
                "Number of times the I/O layer checked for new I/O to perform.", null);

            registerTimeGaugeForObject(meterRegistry, o, "io-time-ns-avg", "io-time-avg", tags,
                "The average length of time for I/O per select call.");
            registerTimeGaugeForObject(meterRegistry, o, "io-wait-time-ns-avg", "io-wait-time-avg", tags,
                "The average length of time the I/O thread spent waiting for a socket to be ready for reads or writes.");
        });

        registerMetricsEventually("producer-topic-metrix", (o, tags) -> {
            registerGaugeForObject(meterRegistry, o, "record-retry-rate", tags, "The number of retry calls per second.",
                "retry");
            registerGaugeForObject(meterRegistry, o, "record-send-rate", tags, "The number of send calls per second.",
                "send");
            registerGaugeForObject(meterRegistry, o, "compression-rate", tags,
                "The number of compression calls per second.", "compression");
            registerGaugeForObject(meterRegistry, o, "byte-rate", tags, "The number of byte(selector) per second.",
                "byte");
            registerGaugeForObject(meterRegistry, o, "record-error-rate", tags, "The number of error calls per second.",
                "error");
        });

        registerMetricsEventually("producer-node-metrix", (o, tags) -> {
            registerGaugeForObject(meterRegistry, o, "request-rate", tags,
                "The average number of requests sent per second.", "request");
            registerGaugeForObject(meterRegistry, o, "response-rate", tags,
                "The average number of responses received per second.", "response");
            registerTimeGaugeForObject(meterRegistry, o, "request-latency-avg", tags,
                "The average time taken for a request.");
            registerTimeGaugeForObject(meterRegistry, o, "request-latency-max", tags,
                "The max time taken for a fetch request.");
            registerGaugeForObject(meterRegistry, o, "incoming-byte-rate", tags,
                "The number of incoming byte(selector) per second.", "incoming-byte");
            registerGaugeForObject(meterRegistry, o, "outgoing-byte-rate", tags,
                "The number of outgoing byte(selector) per second.", "outgoing-byte");
            registerGaugeForObject(meterRegistry, o, "request-size-avg", tags,
                "The max time taken for a fetch request.", "bytes");
            registerGaugeForObject(meterRegistry, o, "request-size-max", tags,
                "The maximum size of any request sent in the window.", "bytes");
        });
    }

    private Iterable<Tag> nameTag(ObjectName name) {
        Tags tags = Tags.empty();
        String clientId = name.getKeyProperty("client-id");
        if (clientId != null) {
            tags = Tags.concat(tags, "client.id", clientId);
        }

        String topic = name.getKeyProperty("topic");
        if (topic != null) {
            tags = Tags.concat(tags, "topic", topic);
        }

        String nodeId = name.getKeyProperty("node-id");
        if (nodeId != null) {
            tags = Tags.concat(tags, "node-id", nodeId);
        }
        return tags;
    }

    private void registerGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, String meterName,
        Tags allTags, String description, @Nullable String baseUnit) {
        Gauge
            .builder(METRIC_NAME_PREFIX + meterName, getMBeanServer(),
                s -> safeDouble(() -> s.getAttribute(o, jmxMetricName)))
            .description(description).baseUnit(baseUnit).tags(allTags).register(registry);
    }

    private void registerGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, Tags allTags,
        String description, @Nullable String baseUnit) {
        registerGaugeForObject(registry, o, jmxMetricName, sanitize(jmxMetricName), allTags, description, baseUnit);
    }

    private void registerMetricsEventually(String type, BiConsumer<ObjectName, Tags> perObject) {
        try {
            Set<ObjectName> objs = mBeanServer.queryNames(new ObjectName(JMX_DOMAIN + ":type=" + type + ",*"), null);
            if (!objs.isEmpty()) {
                for (ObjectName o : objs) {
                    perObject.accept(o, Tags.concat(tags, nameTag(o)));
                }
                return;
            }
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Error registering Kafka JMX based metrics", e);
        }

        NotificationListener notificationListener = new NotificationListener() {

            @Override
            public void handleNotification(Notification notification, Object handback) {
                MBeanServerNotification mbs = (MBeanServerNotification)notification;
                ObjectName o = mbs.getMBeanName();
                perObject.accept(o, Tags.concat(tags, nameTag(o)));
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
            getMBeanServer().addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, notificationListener, filter,
                null);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException("Error registering Kafka MBean listener", e);
        }
    }

    private void registerTimeGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName,
        String meterName, Tags allTags, String description) {
        TimeGauge
            .builder(METRIC_NAME_PREFIX + meterName, getMBeanServer(), TimeUnit.MILLISECONDS,
                s -> safeDouble(() -> s.getAttribute(o, jmxMetricName)))
            .description(description).tags(allTags).register(registry);
    }

    private void registerTimeGaugeForObject(MeterRegistry registry, ObjectName o, String jmxMetricName, Tags allTags,
        String description) {
        registerTimeGaugeForObject(registry, o, jmxMetricName, sanitize(jmxMetricName), allTags, description);
    }

    private double safeDouble(Callable<Object> callable) {
        try {
            return Double.parseDouble(callable.call().toString());
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
