package com.bkjk.platform.monitor.logging.appender.logback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.bkjk.platform.monitor.logging.appender.DefaultKafkaProducerFactory;
import com.bkjk.platform.monitor.logging.appender.KafkaProducerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

public class AdvancedKafkaAppender extends AppenderBase<ILoggingEvent> {
    public static final String DEFAULT_TIMEOUT_MILLIS = "30000";
    private static KafkaProducerFactory producerFactory = new DefaultKafkaProducerFactory();
    private static final ExecutorService SEND_MESSAGE_EXECUTOR = Executors.newSingleThreadExecutor();
    private String topic;
    private String bootstrapServers;
    private boolean failOnStartup = false;
    Producer<byte[], byte[]> producer;
    private Layout<ILoggingEvent> layout;
    private List<String> customProps = new ArrayList<>();
    private final Properties config = new Properties();

    public void addCustomProp(String customProp) {
        customProps.add(customProp);
    }

    @Override
    protected void append(ILoggingEvent event) {
        SEND_MESSAGE_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                if (producer != null) {
                    try {
                        String msg = layout.doLayout(event);
                        final ProducerRecord<byte[], byte[]> newRecord = new ProducerRecord<>(topic, msg.getBytes());
                        producer.send(newRecord);
                    } catch (Exception e) {
                        addWarn("Unable to send message to Kafka", e);
                    }
                }
            }
        });

    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public void start() {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(bootstrapServers, "bootstrapServers must not be null");
        Objects.requireNonNull(layout, "layout must not be null");
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 0);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        try {
            if (producer == null)
                producer = producerFactory.newKafkaProducer(config);
            super.start();
        } catch (Exception e) {
            if (failOnStartup) {
                addError("Unable to start Kafka Producer", e);
            } else {
                addWarn("Unable to start Kafka Producer", e);
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

}
