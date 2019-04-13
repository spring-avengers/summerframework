package com.bkjk.platform.monitor.logging.appender.log4j;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.SerializedLayout;
import org.apache.logging.log4j.core.util.StringEncoder;

@SuppressWarnings("deprecation")
@Plugin(name = "AdvancedKafkaAppender", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class AdvancedKafkaAppender extends AbstractAppender {

    @PluginFactory
    public static AdvancedKafkaAppender createAppender(
        @PluginElement("Layout") final Layout<? extends Serializable> layout,
        @PluginElement("Filter") final Filter filter, //
        @PluginConfiguration final Configuration configuration,
        @Required(message = "No name provided for KafkaAppender") @PluginAttribute("name") final String name,
        @Required(message = "No topic provided for KafkaAppender") @PluginAttribute("topic") final String topic,
        @Required(
            message = "No bootstrapServers provided for KafkaAppender") @PluginAttribute("bootstrapServers") final String bootstrapServers) {
        final AdvancedKafkaManager advancedKafkaManager =
            new AdvancedKafkaManager(configuration.getLoggerContext(), name, bootstrapServers);
        return new AdvancedKafkaAppender(name, layout, filter, false, advancedKafkaManager, topic);
    }

    private final AdvancedKafkaManager manager;

    private final String topic;

    private AdvancedKafkaAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
        final boolean ignoreExceptions, final AdvancedKafkaManager manager, String topic) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = manager;
        this.topic = topic;
    }

    @Override
    public void append(final LogEvent event) {
        try {
            final Layout<? extends Serializable> layout = getLayout();
            byte[] data;
            if (layout != null) {
                if (layout instanceof SerializedLayout) {
                    final byte[] header = layout.getHeader();
                    final byte[] body = layout.toByteArray(event);
                    data = new byte[header.length + body.length];
                    System.arraycopy(header, 0, data, 0, header.length);
                    System.arraycopy(body, 0, data, header.length, body.length);
                } else {
                    data = layout.toByteArray(event);
                }
            } else {
                data = StringEncoder.toBytes(event.getMessage().getFormattedMessage(), StandardCharsets.UTF_8);
            }
            manager.send(topic, data);
        } catch (final Exception e) {
            LOGGER.error("Unable to write to Kafka [{}] for appender [{}].", manager.getName(), getName(), e);
            throw new AppenderLoggingException("Unable to write to Kafka in appender: " + e.getMessage(), e);
        }
    }

    @Override
    public void start() {
        super.start();
        manager.startup();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "KafkaAppender{" + "name=" + getName() + ", state=" + getState() + ", topic=" + topic + '}';
    }
}
