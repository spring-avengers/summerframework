
package com.bkjk.platform.monitor.logging.appender.log4j;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bkjk.platform.monitor.logging.appender.DefaultKafkaProducerFactory;
import com.bkjk.platform.monitor.logging.appender.KafkaProducerFactory;

public class AdvancedKafkaManager extends AbstractManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedKafkaManager.class);
    public static final String DEFAULT_TIMEOUT_MILLIS = "30000";

    private static KafkaProducerFactory producerFactory = new DefaultKafkaProducerFactory();

    private static final ExecutorService SEND_MESSAGE_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Properties config = new Properties();
    private Producer<byte[], byte[]> producer;

    public AdvancedKafkaManager(final LoggerContext loggerContext, final String name, final String bootstrapServers) {
        super(loggerContext, name);
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 0);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
    }

    private void closeProducer(final long timeout, final TimeUnit timeUnit) {
        if (producer != null) {
            final Thread closeThread = new Log4jThread(new Runnable() {
                @Override
                public void run() {
                    if (producer != null) {
                        producer.close();
                    }
                }
            }, "AdvancedKafkaManager-CloseThread");
            closeThread.setDaemon(true);
            closeThread.start();
            try {
                closeThread.join(timeUnit.toMillis(timeout));
            } catch (final InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        if (timeout > 0) {
            closeProducer(timeout, timeUnit);
        }
        return true;
    }

    public void send(final String _topic, final byte[] msg)
        throws ExecutionException, InterruptedException, TimeoutException {
        SEND_MESSAGE_EXECUTOR.submit(new Runnable() {

            @Override
            public void run() {
                if (producer != null) {
                    try {
                        final ProducerRecord<byte[], byte[]> newRecord = new ProducerRecord<>(_topic, msg);
                        producer.send(newRecord);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to send message to Kafka", e);
                    }
                }
            }
        });

    }

    public void startup() {
        producer = producerFactory.newKafkaProducer(config);
    }

}
